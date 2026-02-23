from typing import List

from pydantic import BaseModel


class PredictionItem(BaseModel):
    filename: str
    predicted_index: int
    predicted_label: str
    confidence: float
    topk: List[dict]


class PredictResponse(BaseModel):
    model_name: str
    class_names: List[str]
    count: int
    inference_ms: float
    results: List[PredictionItem]
