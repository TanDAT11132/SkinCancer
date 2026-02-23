# ModelService - FastAPI inference service (behind Spring Boot gateway)

## 1) Role in architecture

- FastAPI service: only does model inference.
- Spring Boot gateway: handles auth, business logic, data persistence, and forwards image files to FastAPI.
- Frontend: calls Spring Boot only (not FastAPI directly).

## 2) Project structure

```text
ModelService/
  app/
    __init__.py
    config.py
    main.py
    model_service.py
    schemas.py
  artifacts/
    best_model.pt
  .env.example
  Dockerfile
  requirements.txt
```

## 3) Prepare checkpoint

Copy trained checkpoint to:

```text
artifacts/best_model.pt
```

Checkpoint must include:
- `model_state_dict`
- `class_names`

## 4) Run FastAPI locally

```powershell
cd d:\vscode\ModelService
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

Health:

```text
http://localhost:8000/health
```

Swagger:

```text
http://localhost:8000/docs
```

## 5) Environment variables

- `MODEL_CKPT_PATH` (default: `artifacts/best_model.pt`)
- `MODEL_NAME` (default: `efficientnet_b3`)
- `IMAGE_SIZE` (default: `300`)
- `DEVICE` (default: `cpu`)
- `MAX_BATCH_SIZE` (default: `32`)

## 6) API contract (inference only)

### `GET /health`

Returns model/service status.

### `POST /v1/predict?top_k=3`

- Content-Type: `multipart/form-data`
- Required field: `files` (repeat for multiple images)
- No user profile fields, no storage side effects.

Example:

```bash
curl -X POST "http://localhost:8000/v1/predict?top_k=3" \
  -F "files=@img1.jpg" \
  -F "files=@img2.jpg"
```

## 7) Docker

```bash
docker build -t skin-model-api:1.2 .
docker run --rm -p 8000:8000 --name skin-model-api skin-model-api:1.2
```
