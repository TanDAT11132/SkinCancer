from io import BytesIO
from typing import List, Tuple

import numpy as np
import torch
import torch.nn as nn
from PIL import Image
from torchvision import models, transforms

from app.config import settings


IMAGENET_MEAN = (0.485, 0.456, 0.406)
IMAGENET_STD = (0.229, 0.224, 0.225)
NON_SKIN_LABEL = "NON_SKIN_IMAGE"
NON_SKIN_REASON = "Input image does not appear to be skin."


class ModelService:
    def __init__(self) -> None:
        self.device = torch.device(settings.device if torch.cuda.is_available() else "cpu")
        self.ckpt = torch.load(settings.model_ckpt_path, map_location=self.device)
        self.class_names = self.ckpt.get("class_names", [])
        if not self.class_names:
            raise ValueError("Missing class_names in checkpoint")

        num_classes = len(self.class_names)
        self.model = self._build_model(num_classes)
        self.model.load_state_dict(self.ckpt["model_state_dict"])
        self.model.to(self.device)
        self.model.eval()

        self.transform = transforms.Compose(
            [
                transforms.Resize(int(settings.image_size * 1.1)),
                transforms.CenterCrop(settings.image_size),
                transforms.ToTensor(),
                transforms.Normalize(IMAGENET_MEAN, IMAGENET_STD),
            ]
        )

    def _build_model(self, num_classes: int) -> nn.Module:
        if settings.model_name != "efficientnet_b3":
            raise ValueError(f"Unsupported model_name: {settings.model_name}")

        model = models.efficientnet_b3(weights=None)
        in_features = model.classifier[1].in_features
        model.classifier[1] = nn.Linear(in_features, num_classes)
        return model

    def preprocess(self, image_bytes: bytes) -> torch.Tensor:
        image = Image.open(BytesIO(image_bytes)).convert("RGB")
        return self.transform(image)

    def _load_rgb(self, image_bytes: bytes) -> Image.Image:
        return Image.open(BytesIO(image_bytes)).convert("RGB")

    def _skin_pixel_ratio(self, image: Image.Image) -> float:
        rgb = np.asarray(image, dtype=np.uint8)
        r = rgb[:, :, 0].astype(np.int16)
        g = rgb[:, :, 1].astype(np.int16)
        b = rgb[:, :, 2].astype(np.int16)

        # RGB rule of thumb for skin-like tones under varied lighting.
        rgb_mask = (
            (r > 95)
            & (g > 40)
            & (b > 20)
            & ((np.maximum(np.maximum(r, g), b) - np.minimum(np.minimum(r, g), b)) > 15)
            & (np.abs(r - g) > 15)
            & (r > g)
            & (r > b)
        )

        ycbcr = np.asarray(image.convert("YCbCr"), dtype=np.uint8)
        cb = ycbcr[:, :, 1].astype(np.int16)
        cr = ycbcr[:, :, 2].astype(np.int16)
        ycbcr_mask = (cb >= 77) & (cb <= 127) & (cr >= 133) & (cr <= 173)

        mask = rgb_mask & ycbcr_mask
        return float(mask.mean())

    def _is_skin_like_image(self, image: Image.Image) -> bool:
        return self._skin_pixel_ratio(image) >= settings.min_skin_ratio

    @torch.inference_mode()
    def predict_batch(
        self, image_items: List[Tuple[str, bytes]], top_k: int = 1
    ) -> List[dict]:
        if len(image_items) > settings.max_batch_size:
            raise ValueError(f"Too many images. max_batch_size={settings.max_batch_size}")

        k = max(1, min(top_k, len(self.class_names)))
        results = [{} for _ in image_items]

        valid_positions = []
        valid_tensors = []
        for idx, (filename, content) in enumerate(image_items):
            image = self._load_rgb(content)
            if not self._is_skin_like_image(image):
                results[idx] = {
                    "filename": filename,
                    "predicted_index": -1,
                    "predicted_label": NON_SKIN_LABEL,
                    "confidence": 0.0,
                    "topk": [],
                    "is_valid_skin_image": False,
                    "rejection_reason": NON_SKIN_REASON,
                }
                continue
            valid_positions.append(idx)
            valid_tensors.append(self.transform(image))

        if not valid_tensors:
            return results

        batch = torch.stack(valid_tensors).to(self.device)
        logits = self.model(batch)
        probs = torch.softmax(logits, dim=1)
        confs, indices = torch.topk(probs, k=k, dim=1)

        for row, original_pos in enumerate(valid_positions):
            filename = image_items[original_pos][0]
            topk_items = []
            for col in range(k):
                idx = int(indices[row, col].item())
                topk_items.append(
                    {
                        "index": idx,
                        "label": self.class_names[idx],
                        "confidence": float(confs[row, col].item()),
                    }
                )

            results[original_pos] = {
                "filename": filename,
                "predicted_index": topk_items[0]["index"],
                "predicted_label": topk_items[0]["label"],
                "confidence": topk_items[0]["confidence"],
                "topk": topk_items,
                "is_valid_skin_image": True,
                "rejection_reason": None,
            }
        return results
