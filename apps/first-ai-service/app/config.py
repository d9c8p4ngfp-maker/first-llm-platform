import os
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "first-ai-service"
    app_version: str = "0.1.0"
    rag_data_dir: str = os.getenv("RAG_DATA_DIR", "D:/first/_local/data/rag")

    pg_host: str = "localhost"
    pg_port: int = 5433
    pg_db: str = "first_rag"
    pg_user: str = "rag"
    pg_password: str = ""


settings = Settings()
