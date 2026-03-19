# SkinCancer Backend (Spring Boot)

## Run backend (serve ca FE + API)

```bash
mvn spring-boot:run
```

- Backend port: `8001`
- UI URL: `http://localhost:8001/index.html`
- API base (same origin): `http://localhost:8001`

## Upload storage

- Upload images luu tai: `D:/SkinCancer/uploads`
- Config tai `src/main/resources/application.yml`:
  - `app.upload.root-dir: D:/SkinCancer/uploads`

## Frontend + Backend gop chung

Frontend da duoc copy vao `src/main/resources/static`:

- `src/main/resources/static/index.html`
- `src/main/resources/static/app.js`
- `src/main/resources/static/style.css`
- `src/main/resources/static/config.js`

Trong `config.js`:

- `API_BASE_URL` dang de `""` de goi cung domain/port voi backend.
- `GOOGLE_CLIENT_ID` can set gia tri that.

## Chay 2 project trong VS Code

Da them task: `.vscode/tasks.json`

- `Run Spring Boot`
- `Run FastAPI`
- `Run BE + FastAPI` (parallel)

## APIs flow

1. `POST /api/auth/google`
2. `GET /api/me`
3. `PUT /api/me/profile`
4. `POST /api/predictions` (alias cu: `POST /api/predictions/check`)
5. `GET /api/predictions?page=0&size=20` (alias cu: `GET /api/predictions/history?page=0&size=20`)
6. `POST /api/predictions/{predictionId}/feedbacks` (alias cu: `POST /api/predictions/{predictionId}/feedback`)
7. `GET /api/predictions/retrain-samples?page=0&size=100` (ADMIN)

## Error format (Backend -> Frontend)

```json
{
  "timestamp": "2026-02-23T...",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "age must be less than or equal to 120",
  "path": "/api/me/profile"
}
```

Frontend parse `code/message` va hien thi tren `errorBanner`.
