from dataclasses import dataclass
from uuid import UUID
from pydantic import BaseModel, Field, ConfigDict


@dataclass(frozen=True, slots=True)
class Evidence:
    document_id: UUID
    text: str


@dataclass(frozen=True, slots=True)
class RetrievedItem:
    document_id: UUID
    content: str


@dataclass(frozen=True, slots=True)
class RetrievalMetrics:
    hit_at_k: float
    recall_at_k: float
    mrr: float


class AnswerJudgment(BaseModel):
    """外部评审模型返回值，在此验证类型和评分范围。"""
    model_config = ConfigDict(extra="forbid")
    answer_relevance: float = Field(ge=0, le=1, description="是否回答了问题")
    answer_correctness: float | None = Field(ge=0, le=1, description="与参考答案一致程度，无参考答案为空")
    faithfulness: float = Field(ge=0, le=1, description="回答声明受实际上下文支持程度")
    unsupported_claims: list[str] = Field(description="不受上下文支持的声明")
    reason: str = Field(min_length=1, max_length=4000, description="评分理由")
