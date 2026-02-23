from io import BytesIO
from typing import List, Tuple

import torch
import torch.nn as nn
from PIL import Image
from torchvision import models, transforms

from app.config import settings


IMAGENET_MEAN = (0.485, 0.456, 0.406)
IMAGENET_STD = (0.229, 0.224, 0.225)


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

    @torch.inference_mode()
    def predict_batch(
        self, image_items: List[Tuple[str, bytes]], top_k: int = 1
    ) -> List[dict]:
        if len(image_items) > settings.max_batch_size:
            raise ValueError(f"Too many images. max_batch_size={settings.max_batch_size}")

        tensors = [self.preprocess(content) for _, content in image_items]
        batch = torch.stack(tensors).to(self.device)

        logits = self.model(batch)
        probs = torch.softmax(logits, dim=1)
        k = max(1, min(top_k, len(self.class_names)))
        confs, indices = torch.topk(probs, k=k, dim=1)

        results = []
        for row in range(len(image_items)):
            filename = image_items[row][0]
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

            results.append(
                {
                    "filename": filename,
                    "predicted_index": topk_items[0]["index"],
                    "predicted_label": topk_items[0]["label"],
                    "confidence": topk_items[0]["confidence"],
                    "topk": topk_items,
                }
            )
        return results
