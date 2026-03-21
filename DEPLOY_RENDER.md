# Deploy Render Free - Tung buoc

## 1) Muc tieu deploy

Project nay deploy theo 2 web services:

1. `backend`: Spring Boot + frontend static
2. `modelservice`: FastAPI + PyTorch

Va 1 database Postgres.

Render free dung duoc cho demo, nhung can chap nhan:

- Service ngu sau 15 phut khong co request
- Luc wake up co the cham
- Gio free bi gioi han theo workspace
- Disk cua web service khong ben vung, file upload co the mat sau restart/redeploy

## 2) Cai can cai tren may

1. Docker Desktop
2. Git
3. Tai khoan Render
4. Tai khoan Postgres free:
   - de don gian: Render Postgres free
   - de ben hon free: Neon hoac Supabase

Khong bat buoc phai cai Maven/JDK neu ban build bang Docker.

## 3) Chay local bang Docker truoc khi deploy

Tu thu muc goc repo:

```powershell
docker compose build
docker compose up
```

Kiem tra:

- Backend UI: `http://localhost:8001/index.html`
- Backend health: `http://localhost:8001/api/health`
- Model health: `http://localhost:8000/health`

Neu muon dung DB moi tinh cho demo, compose da tu tao Postgres local.

## 4) Bien moi truong can cho backend tren Render

Dat cac bien sau cho service `backend`:

```text
PORT=10000
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>:5432/<db>
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_JPA_HIBERNATE_DDL_AUTO=update
APP_JWT_SECRET=<chuoi-random-dai-it-nhat-32-ky-tu>
APP_FASTAPI_BASE_URL=https://<modelservice>.onrender.com
APP_FASTAPI_DEFAULT_TOP_K=3
APP_UPLOAD_ROOT_DIR=/tmp/uploads
APP_ADMIN_EMAIL=<email-admin>
APP_GOOGLE_CLIENT_ID=<google-oauth-web-client-id>
APP_CORS_ALLOWED_ORIGINS=https://<backend-service>.onrender.com
```

Ghi chu:

- Lan deploy dau tien, de `SPRING_JPA_HIBERNATE_DDL_AUTO=update` de app tu tao bang
- Sau khi chay on, co the doi lai `validate`
- `APP_UPLOAD_ROOT_DIR=/tmp/uploads` chi phu hop demo, vi file upload khong duoc luu ben vung
- `APP_FASTAPI_BASE_URL` co the la URL day du `https://...` hoac gia tri noi bo dang `host:port`
- Frontend se doc `GOOGLE_CLIENT_ID` dong tu backend qua `/config.js`

## 5) Bien moi truong cho modelservice tren Render

Dat cac bien sau cho service `modelservice`:

```text
PORT=10000
DEVICE=cpu
MODEL_CKPT_PATH=artifacts/best_model.pt
MODEL_NAME=efficientnet_b3
IMAGE_SIZE=300
MAX_BATCH_SIZE=32
MIN_SKIN_RATIO=0.15
```

## 6) Tao database

Ban co 2 cach:

### Cach A: Render Postgres free

- Tao Postgres tren Render
- Lay `Internal Database URL` hoac thong tin host/user/pass/db
- Gan vao bien moi truong cua backend

Luu y: goi free cua Render Postgres co gioi han va khong phu hop luu lau dai.

### Cach B: Neon hoac Supabase

- Tao Postgres free ben ngoai
- Lay connection string
- Gan vao `SPRING_DATASOURCE_URL`

Cach nay thuong on hon cho demo keo dai.

## 7) Tao service modelservice tren Render

1. Push repo len GitHub
2. Trong Render, chon `New +` -> `Web Service`
3. Chon repo nay
4. Chon runtime `Docker`
5. Dat:
   - Name: `skincancer-model`
   - Root directory: `ModelService`
6. Render se tu nhan `Dockerfile`
7. Them cac env vars cua modelservice
8. Health check path: `/health`
9. Deploy

Sau khi xong, luu lai URL dang:

```text
https://skincancer-model.onrender.com
```

## 8) Tao service backend tren Render

1. Tao them `Web Service`
2. Chon cung repo
3. Runtime `Docker`
4. Dat:
   - Name: `skincancer-backend`
   - Root directory: `Backend`
5. Them env vars cua backend
6. Dat `APP_FASTAPI_BASE_URL` bang URL model service o buoc 7
7. Health check path: `/api/health`
8. Deploy

Sau khi xong, frontend se truy cap tu:

```text
https://skincancer-backend.onrender.com/index.html
```

Hoac ban co the deploy bang Blueprint tu file `render.yaml` o thu muc goc repo de tao ca 2 services va database trong mot lan.

## 9) Google Login

Neu dung Google login, vao Google Cloud Console va them:

- Authorized JavaScript origins:

```text
https://<backend-service>.onrender.com
```

Neu khong them origin nay, login Google se loi.

## 10) Database co can lam sach khong

Khong can don local DB.

Ban nen:

1. Tao DB cloud moi
2. Cho backend tu tao bang lan dau voi:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

3. Sau khi app da tao bang va on dinh, co the doi lai:

```text
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

## 11) Du lieu upload co mat khong

Co the mat.

Voi Render free, upload dang luu vao filesystem tam cua container. Khi service restart, scale, hoac redeploy, file cu co the khong con.

Dieu nay khong anh huong viec demo predict ngay luc upload, nhung se anh huong lich su anh neu ban can mo lai file cu.

Neu can ben hon, buoc sau nen doi sang cloud storage nhu Cloudinary, Supabase Storage, hoac S3-compatible storage.

## 12) Trinh tu deploy ngan gon

1. Push code len GitHub
2. Tao Postgres free
3. Deploy `modelservice`
4. Lay URL model service
5. Deploy `backend`
6. Cau hinh Google origin
7. Test:
   - `/api/health`
   - `/health`
   - upload quick-check
   - login Google
