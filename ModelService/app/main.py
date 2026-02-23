import time
from typing import List

from fastapi import FastAPI, File, HTTPException, Query, UploadFile

from app.config import settings
from app.model_service import ModelService
from app.schemas import PredictResponse

app = FastAPI(title="Skin Cancer Model API", version="1.2.0")
service = ModelService()


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "device": str(service.device),
        "model": settings.model_name,
        "num_classes": len(service.class_names),
    }


@app.post("/v1/predict", response_model=PredictResponse)
async def predict(
    files: List[UploadFile] = File(..., description="One or many image files"),
    top_k: int = Query(default=1, ge=1, le=10),
) -> PredictResponse:
    if not files:
        raise HTTPException(status_code=400, detail="No files uploaded")

    payload = []
    for f in files:
        content = await f.read()
        if not content:
            raise HTTPException(status_code=400, detail=f"Empty file: {f.filename}")
        payload.append((f.filename or "unknown", content))

    start = time.perf_counter()
    try:
        results = service.predict_batch(payload, top_k=top_k)
    except ValueError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
    except Exception as ex:
        raise HTTPException(status_code=500, detail=f"Inference error: {ex}") from ex

    infer_ms = (time.perf_counter() - start) * 1000.0
    return PredictResponse(
        model_name=settings.model_name,
        class_names=service.class_names,
        count=len(results),
        inference_ms=round(infer_ms, 3),
        results=results,
    )
