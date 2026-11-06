from typing import Annotated
from uuid import UUID

from pydantic import Field, StringConstraints

from app.schemas.base import BaseSchema


class RetrievalPreviewRequest(BaseSchema):
    query: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=4000),
        Field(description="检索问题"),
    ]
    top_k: int = Field(default=5, ge=1, le=20, description="召回文档数量")


class RetrievalPreviewItem(BaseSchema):
    chunk_id: UUID = Field(description="片段 ID")
    document_id: UUID = Field(description="文档 ID")
    file_name: str = Field(description="文件名")
    content: str = Field(description="命中的片段内容")
    page_from: int | None = Field(default=None, description="起始页码")
    page_to: int | None = Field(default=None, description="结束页码")
    section_title: str | None = Field(default=None, description="章节标题")
    similarity: float = Field(description="向量相似度")
    generation: int = Field(exclude=True, description="索引版本")


class RetrievalPreviewResponse(BaseSchema):
    knowledge_base_id: UUID = Field(description="知识库 ID")
    query: str = Field(description="实际检索问题")
    items: list[RetrievalPreviewItem] = Field(description="检索结果")
    elapsed_ms: float = Field(description="检索耗时，单位为毫秒")
