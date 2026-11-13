from typing import Literal

from pydantic import Field, field_validator

from app.page_generation.schemas import PagePreview, PageSpec
from app.schemas.base import BaseSchema


class PageAgentWorkspaceMessage(BaseSchema):
    role: Literal["user", "assistant"] = Field(description="消息角色")
    content: str = Field(min_length=1, max_length=8000, description="消息内容")
    reasoning_content: str = Field(default="", description="模型返回的独立推理内容，与回复正文分开保存")

    @field_validator("content")
    @classmethod
    def strip_content(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("消息内容不能为空")
        return value


class PageAgentWorkspaceResponse(BaseSchema):
    messages: list[PageAgentWorkspaceMessage] = Field(default_factory=list, max_length=40, description="工作区消息")
    page_spec: PageSpec | None = Field(default=None, description="当前页面结构")
    preview: PagePreview | None = Field(default=None, description="当前页面预览")


class PageAgentMessageRequest(BaseSchema):
    content: str = Field(min_length=1, max_length=4000, description="页面需求")

    @field_validator("content")
    @classmethod
    def strip_content(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("页面需求不能为空")
        return value
