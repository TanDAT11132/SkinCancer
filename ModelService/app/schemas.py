from typing import List, Optional

from pydantic import BaseModel


class TopKItem(BaseModel):
    index: int
    label: str
    confidence: float


class PredictionItem(BaseModel):
    filename: str
    predicted_index: int
    predicted_label: str
    confidence: float
    topk: List[TopKItem]
    is_valid_skin_image: bool
    rejection_reason: Optional[str] = None


class PredictResponse(BaseModel):
    model_name: str
    class_names: List[str]
    count: int
    inference_ms: float
    results: List[PredictionItem]
