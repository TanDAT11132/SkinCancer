"# SkinCancer" 
Project phân loại ung thư da, bộ **HAM10000** với 7 lớp:
`akiec, bcc, bkl, df, mel, nv, vasc`.
Mục tiêu chính: đạt hiệu quả tốt trong bối cảnh **mất cân bằng lớp mạnh** (đặc biệt lớp `nv` rất nhiều) và giảm overfitting trên dataset y tế tương đối nhỏ.
https://drive.google.com/file/d/1bf-QJ7b1p-vbnM5X_oALFXvML2DPu-HR/view?usp=sharing
## 1)  Pipeline
- **Data**: chuẩn `torchvision.datasets.ImageFolder` theo 3 tập `train/val/test`.
- **Backbone**: `EfficientNet-B3` pretrained ImageNet (Transfer Learning).
- **Train 2 giai đoạn**:
  1. **Freeze backbone** và train classifier (head) vài epoch để “khớp” nhanh với bài toán 7 lớp.
  2. **Unfreeze toàn bộ** và fine-tune với learning rate nhỏ hơn cho backbone để tránh “quên kiến thức pretrained”.
- **Tối ưu cho imbalance**:
  - `WeightedRandomSampler` để cân bằng tần suất xuất hiện lớp trong batch.
  - `Focal Loss` để tập trung vào mẫu khó / lớp hiếm.
- **Tăng generalization**:
  - Augmentation mạnh + `Mixup`
  - `Label smoothing`, `AdamW + weight decay`
  - Scheduler cosine warm restarts
  - `Early stopping`, `AMP`, `Gradient clipping`
## 3) Tiền xử lý & Augmentation
### 3.1 Normalize theo ImageNet
### 3.2 Augmentation train (giảm overfit)
đa dạng tỷ lệ/khung nhìn, tránh học “vị trí cố định”.
ảnh dermoscopy có thể xoay/đảo, tăng robustness.
tăng khả năng nhận dạng pattern không phụ thuộc góc.
mô phỏng thay đổi ánh sáng/camera/skin tone.
### 3.3 Validation/Test transform (ổn định đánh giá)
- Resize + CenterCrop → giảm nhiễu augmentation để metric phản ánh đúng chất lượng.
## 4) Xử lý mất cân bằng lớp (Imbalance Handling)
### 4.1 WeightedRandomSampler (cân bằng dữ liệu theo batch)
### 4.2 Focal Loss (tập trung vào mẫu khó)
 Focal Loss cho multi-class:
- CE từng mẫu: `ce_i`
## 5) Mixup (Regularization mạnh)
Mixup tạo ảnh mới
## 6) Chiến lược Training (2-stage Fine-tuning)
 Stage 1: Train head (freeze backbone)
 Stage 2: Fine-tune toàn bộ (unfreeze backbone)
## 7) Tối ưu hóa & Ổn định training
### 7.1 Optimizer: AdamW
- AdamW xử lý weight decay đúng cách → regularization hiệu quả hơn cho deep nets.
### ModelService
FastAPI này triển khai **service inference** cho mô hình phân loại tổn thương da (HAM10000 – 7 lớp).  
Nhiệm vụ: **nhận ảnh từ backend Spring Boot và trả về kết quả dự đoán (top-k + confidence)**.
  ## 1) Tính năng
- **Health check** để kiểm tra service và thông tin model/device.
- **Predict**: nhận **1 hoặc nhiều ảnh** qua `multipart/form-data`, trả về:
  - nhãn dự đoán (`predicted_label`)
  - chỉ số lớp (`predicted_index`)
  - độ tin cậy (`confidence`)
  - danh sách `topk` (top-k lớp có xác suất cao nhất)
- Giới hạn batch: `max_batch_size` (mặc định 32 ảnh/request).
  ## 2) Cấu hình
Service đọc config từ biến môi trường (.env) qua `pydantic-settings`.

Các biến chính (mặc định trong code):
- `MODEL_CKPT_PATH` (default: `artifacts/best_model.pt`)
- `MODEL_NAME` (default: `efficientnet_b3`)
- `IMAGE_SIZE` (default: `300`)
- `DEVICE` (default: `cpu`) *(nếu có CUDA thì vẫn ưu tiên GPU khi `torch.cuda.is_available()` true)*
- `MAX_BATCH_SIZE` (default: `32`)
### backend
- Cung cấp **REST API** cho đăng nhập Google, quản lý user, lưu lịch sử dự đoán
- Nhận ảnh từ client, **lưu file + lưu DB**, sau đó **gọi FastAPI ModelService** để lấy kết quả dự đoán

## 1) Kiến trúc & luồng xử lý

### Thành phần
- **Frontend (static)**: `index.html`, `app.js`, `style.css`, `config.js`  
  → được Spring Boot serve trực tiếp.
- **Backend (Spring Boot)**: API + Security (JWT) + Database (SQL Server) + Upload storage
- **ModelService (FastAPI)**: endpoint dự đoán ảnh (ví dụ `/v1/predict`)

### Luồng
1. Client đăng nhập Google → lấy `id_token`
2. Client gọi `POST /api/auth/google` → Backend verify token với Google (`tokeninfo`)
3. Backend tạo **JWT** và trả về `accessToken`
4. Client upload ảnh:
   - Có auth: `POST /api/predictions/check`
   - Không auth (demo): `POST /api/predictions/quick-check`
5. Backend:
   - Lưu file vào ổ đĩa (theo ngày `YYYY/MM/DD/uuid.ext`)
   - Lưu metadata vào DB (hash, size, uri…)
   - Gọi FastAPI: `POST {FASTAPI_BASE_URL}/v1/predict?top_k=...` với form-data `files`
   - Lưu prediction vào DB (predictedClass, probability, topKJson, rawResponseJson…)
6. Client xem lịch sử: `GET /api/predictions/history`
7. User gửi feedback: `POST /api/predictions/{predictionId}/feedback`
8. Admin export mẫu retrain: `GET /api/predictions/retrain-samples`
