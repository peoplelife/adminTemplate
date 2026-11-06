from datetime import datetime
from typing import Annotated, Literal
from uuid import UUID

from pydantic import Field, StringConstraints, model_validator

from app.schemas.base import BaseSchema

Text = Annotated[str, StringConstraints(strip_whitespace=True, min_length=1)]


class EvaluationDatasetRequest(BaseSchema):
    name: Text = Field(max_length=100, description="评测集名称")
    knowledge_base_id: UUID = Field(description="所属知识库 ID")
    description: str = Field(default="", max_length=1000, description="说明")


class EvaluationDatasetResponse(EvaluationDatasetRequest):
    id: UUID = Field(description="评测集 ID")
    created_at: datetime = Field(description="创建时间")


class ExpectedEvidence(BaseSchema):
    document_id: UUID = Field(description="预期文档 ID")
    text: Text = Field(max_length=4000, description="文档中可定位的原文依据")


class EvaluationCaseRequest(BaseSchema):
    question: Text = Field(max_length=2000, description="评测问题")
    category: str = Field(default="", max_length=100, description="自定义分类")
    difficulty: Literal["easy", "medium", "hard"] = Field(default="easy", description="难度")
    answerable: bool = Field(default=True, description="知识库是否有答案")
    expected_evidence: list[ExpectedEvidence] = Field(default_factory=list, max_length=20, description="标准原文依据")
    expected_answer: str | None = Field(default=None, max_length=4000, description="可选参考答案")
    notes: str = Field(default="", max_length=2000, description="标注说明")

    @model_validator(mode="after")
    def check_evidence(self):
        if self.answerable != bool(self.expected_evidence):
            raise ValueError("有答案题必须提供依据，无答案题不应提供依据")
        keys = [(item.document_id, "".join(item.text.split())) for item in self.expected_evidence]
        if len(set(keys)) != len(keys):
            raise ValueError("原文依据不能重复")
        return self


class EvaluationCaseResponse(EvaluationCaseRequest):
    id: UUID = Field(description="样本 ID")
    dataset_id: UUID = Field(description="评测集 ID")
    created_at: datetime = Field(description="创建时间")


class EvaluationRunRequest(BaseSchema):
    top_k: int = Field(default=5, ge=1, le=20, description="召回数量")
    generate_answers: bool = Field(default=False, description="是否调用答案生成器评估最终回答")


class EvaluationMetrics(BaseSchema):
    hit_at_k: float | None = Field(default=None, description="至少命中一条依据的题目比例")
    recall_at_k: float | None = Field(default=None, description="每题依据召回率的均值")
    mrr: float | None = Field(default=None, description="首条相关结果排名倒数的均值")
    evaluated_cases: int = Field(default=0, description="成功评测的有答案题数")
    unanswerable_cases: int = Field(default=0, description="无答案题数，仅观察召回不评分")
    failed_cases: int = Field(default=0, description="执行失败的题数")
    generated_cases: int = Field(default=0, description="成功生成答案的题数")
    generation_failed_cases: int = Field(default=0, description="答案生成失败的题数")
    answer_relevance: float | None = Field(default=None, description="答案相关性均值")
    answer_correctness: float | None = Field(default=None, description="答案正确性均值")
    faithfulness: float | None = Field(default=None, description="答案忠实性均值")


class EvaluationResultResponse(BaseSchema):
    id: UUID = Field(description="结果 ID")
    case_id: UUID | None = Field(description="样本 ID，删除后为空")
    case_snapshot: EvaluationCaseRequest = Field(description="执行时的样本快照")
    status: Literal["completed", "failed"] = Field(description="执行状态")
    retrieved_items: list["EvaluationRetrievedItem"] = Field(description="实际召回片段快照")
    hit_at_k: float | None = Field(description="是否命中依据，无答案题或失败时为空")
    recall_at_k: float | None = Field(description="标准依据召回率")
    mrr: float | None = Field(description="首条相关片段排名倒数")
    elapsed_ms: float = Field(description="执行耗时")
    error_code: int | None = Field(description="失败业务码")
    generated_answer: str | None = Field(default=None, description="生成答案")
    generation_model: str | None = Field(default=None, description="生成模型")
    prompt_version: str | None = Field(default=None, description="实际发送的 Prompt 版本")
    generation_status: Literal["not_requested", "completed", "failed"] = Field(default="not_requested", description="答案生成状态")
    answer_relevance: float | None = Field(default=None, description="答案相关性")
    answer_correctness: float | None = Field(default=None, description="答案正确性")
    faithfulness: float | None = Field(default=None, description="答案忠实性")
    unsupported_claims: list[str] = Field(default_factory=list, description="未被上下文支持的声明")
    generation_details: dict = Field(default_factory=dict, description="实际上下文、评审状态、模型、版本及评分理由")


class EvaluationRetrievedItem(BaseSchema):
    chunk_id: UUID = Field(description="执行时的片段 ID")
    document_id: UUID = Field(description="来源文档 ID")
    generation: int = Field(description="执行时的索引版本")
    content: str = Field(description="片段原文快照")
    file_name: str = Field(description="文件名")
    similarity: float = Field(description="相似度")
    page_from: int | None = Field(default=None, description="起始页")
    page_to: int | None = Field(default=None, description="结束页")
    section_title: str | None = Field(default=None, description="章节")


class EvaluationRunResponse(BaseSchema):
    id: UUID = Field(description="运行 ID")
    dataset_id: UUID = Field(description="评测集 ID")
    status: Literal["running", "completed", "partial", "failed"] = Field(description="状态")
    top_k: int = Field(description="召回数量")
    total_cases: int = Field(description="执行样本数")
    metrics: EvaluationMetrics = Field(description="汇总指标，失败题不按零分计算")
    config: dict = Field(description="模型、评分规则和索引配置快照")
    created_at: datetime = Field(description="开始时间")
    completed_at: datetime | None = Field(description="结束时间")


class EvaluationReportResponse(EvaluationRunResponse):
    results: list[EvaluationResultResponse] = Field(description="单题结果")
