from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "first-ai-service"
    app_version: str = "0.1.0"
    rag_data_dir: str = "D:/first/_local/data/rag"


settings = Settings()