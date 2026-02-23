from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    model_ckpt_path: str = "artifacts/best_model.pt"
    model_name: str = "efficientnet_b3"
    image_size: int = 300
    device: str = "cpu"
    max_batch_size: int = 32


settings = Settings()
