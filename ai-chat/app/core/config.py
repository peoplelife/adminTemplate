from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str =Field(default="AI Agent Course API")
    app_env: str = Field(default="development")
    app_version: str = Field(default="0.1.0")
    api_v1_prefix: str = Field(default="/api/v1")
    debug: bool = Field(default=True)

    rag_service_token: str = Field(default="")
    rag_document_root: str = Field(default="")
    rag_embedding_base_url: str = Field(default="")
    rag_embedding_api_key: str = Field(default="")
    rag_embedding_model: str = Field(default="BAAI/bge-m3")
    rag_embedding_timeout_seconds: float = Field(default=30)
    rag_embedding_batch_size: int = Field(default=32)
    rag_rerank_enabled: bool = Field(default=False)
    rag_rerank_base_url: str = Field(default="https://api.siliconflow.cn/v1")
    rag_rerank_api_key: str = Field(default="")
    rag_rerank_model: str = Field(default="BAAI/bge-reranker-v2-m3")
    rag_rerank_candidate_k: int = Field(default=20, ge=1, le=100)
    rag_rerank_timeout_seconds: float = Field(default=30, gt=0)

    deepseek_api_key: str = Field(default="")
    deepseek_base_url: str = Field(default="https://api.deepseek.com")

    default_model: str = Field(default="deepseek-chat")
    supported_models: str = Field(default="deepseek-chat,qwen-plus,gpt-4o-mini")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )
    database_url: str = Field(
        default="postgresql+asyncpg://postgres:postgres@localhost:5432/ai_agent_course"
    )

    database_pool_size: int = Field(default=5, ge=1)
    database_max_overflow: int = Field(default=10, ge=0)

    redis_url: str = Field(default="redis://:redis123@localhost:6379/0")
    redis_default_ttl_seconds: int = Field(default=300)
    rate_limit_window_seconds: int = Field(default=60)
    rate_limit_max_requests: int = Field(default=20)

    default_llm_provider: str = Field(default="deepseek")

    openai_api_key: str = Field(default="")
    openai_base_url: str = Field(default="https://api.openai.com/v1")


    dashscope_api_key: str = Field(default="")
    qwen_base_url: str = Field(
        default="https://dashscope.aliyuncs.com/compatible-mode/v1"
    )


    @property
    def supported_model_list(self) -> list[str]:
        return [
            model.strip()
            for model in self.supported_models.split(",")
            if model.strip()
        ]


@lru_cache
def get_settings() -> Settings:
    return Settings()

settings = get_settings()
