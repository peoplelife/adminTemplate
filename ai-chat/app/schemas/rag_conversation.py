from datetime import datetime
from typing import Annotated, Literal
from uuid import UUID

from pydantic import Field, StringConstraints

from app.schemas.base import BaseSchema


class RagConversationCreateRequest(BaseSchema):
    title: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=200),
        Field(description="会话标题"),
    ] | None = None
    knowledge_base_id: UUID | None = Field(default=None, description="绑定的知识库 ID")
    model: str | None = Field(default=None, max_length=100, description="生成模型名称")


class RagConversationUpdateRequest(BaseSchema):
    title: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=200),
        Field(description="新的会话标题"),
    ] | None = None
    status: Literal["active", "archived"] | None = Field(default=None, description="会话状态")


class RagConversationResponse(BaseSchema):
    conversation_id: UUID = Field(description="会话 ID")
    title: str = Field(description="会话标题")
    knowledge_base_id: UUID | None = Field(default=None, description="绑定的知识库 ID")
    model: str = Field(description="生成模型名称")
    status: str = Field(description="会话状态")
    created_at: datetime = Field(description="创建时间")
    updated_at: datetime = Field(description="更新时间")


class RagMessageRequest(BaseSchema):
    client_request_id: UUID = Field(description="客户端请求幂等 ID")
    content: Annotated[
        str, StringConstraints(strip_whitespace=True, min_length=1, max_length=4000),
        Field(description="用户消息内容"),
    ]


class RagMessageSourceResponse(BaseSchema):
    citation_index: int = Field(description="引用序号")
    document_id: UUID = Field(description="文档 ID")
    document_name: str = Field(description="文档名称")
    page_from: int | None = Field(default=None, description="起始页码")
    page_to: int | None = Field(default=None, description="结束页码")
    section_title: str | None = Field(default=None, description="章节标题")
    excerpt: str = Field(description="引用片段")
    available: bool = Field(default=True, description="原始文档是否仍可用")


class RagMessageResponse(BaseSchema):
    id: UUID = Field(description="消息 ID")
    seq: int = Field(description="会话内消息序号")
    role: str = Field(description="消息角色")
    content: str = Field(description="消息内容")
    status: str = Field(description="消息状态")
    sources: list[RagMessageSourceResponse] = Field(default_factory=list, description="引用来源")
    created_at: datetime = Field(description="创建时间")


class RagMessagePage(BaseSchema):
    items: list[RagMessageResponse] = Field(description="消息列表")
    next_cursor: str | None = Field(default=None, description="下一批历史消息的序号")
