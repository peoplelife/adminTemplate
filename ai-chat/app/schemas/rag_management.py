from datetime import datetime
from typing import Annotated
from uuid import UUID

from pydantic import Field, StringConstraints

from app.schemas.base import BaseSchema


class KnowledgeBaseCreateRequest(BaseSchema):
    name: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=100),
        Field(description="知识库名称"),
    ]
    description: Annotated[
        str, StringConstraints(strip_whitespace=True, max_length=1000),
        Field(description="知识库描述"),
    ] | None = None


class KnowledgeBaseResponse(BaseSchema):
    id: UUID = Field(description="知识库 ID")
    name: str = Field(description="知识库名称")
    description: str | None = Field(default=None, description="知识库描述")
    status: str = Field(description="知识库状态")
    embedding_model: str = Field(description="Embedding 模型名称")
    embedding_dimension: int = Field(description="向量维度")
    chunk_size: int = Field(description="切块大小")
    chunk_overlap: int = Field(description="切块重叠长度")
    document_count: int = Field(default=0, description="文档总数")
    ready_document_count: int = Field(default=0, description="已完成索引的文档数")
    created_at: datetime = Field(description="创建时间")
    updated_at: datetime = Field(description="更新时间")


class DocumentResponse(BaseSchema):
    id: UUID = Field(description="文档 ID")
    knowledge_base_id: UUID = Field(description="所属知识库 ID")
    file_name: str = Field(description="文件名")
    mime_type: str = Field(description="文件 MIME 类型")
    file_size: int = Field(description="文件大小，单位为字节")
    status: str = Field(description="文档状态")
    active_generation: int = Field(description="当前生效索引版本")
    latest_generation: int = Field(description="最新索引版本")
    chunk_count: int = Field(description="当前版本切块数量")
    page_count: int | None = Field(default=None, description="页数")
    created_at: datetime = Field(description="创建时间")
    updated_at: datetime = Field(description="更新时间")
