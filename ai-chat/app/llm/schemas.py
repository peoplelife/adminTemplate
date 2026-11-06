from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class LLMProviderName(str,Enum):
    OPENAI = "openai"
    DEEPSEEK="deepseek"
    QWEN="qwen"

class LLMRole(str,Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"
    TOOL = "tool"

class LLMToolCall(BaseModel):
    id: str = Field(..., description="Tool Call ID")
    name: str = Field(..., description="工具名称")
    arguments: dict[str, Any] = Field(
        default_factory=dict,
        description="工具参数",
    )
    raw_arguments: str = Field(
        default="{}",
        description="模型返回的原始参数字符串",
    )


class LLMToolResultMessage(BaseModel):
    tool_call_id: str = Field(..., description="Tool Call ID")
    name: str = Field(..., description="工具名称")
    content: str = Field(..., description="工具执行结果 JSON 字符串")

class LLMMessage(BaseModel):
    role: LLMRole = Field(..., description="消息角色")
    content: str = Field(..., min_length=0, description="消息内容")
    tool_calls: list[dict[str, Any]]=Field(default=[],description="需要使用的 tool call")
    tool_call_id: str | None = Field(
        default=None,
        description="对应的工具调用 ID",
    )

class LLMRequest(BaseModel):
    model: str = Field(..., description="模型名称")
    messages: list[LLMMessage] = Field(..., min_length=1, description="消息列表")
    temperature: float = Field(default=0.7, ge=0, le=2, description="温度")
    max_tokens: int | None = Field(default=None, ge=1, description="最大输出 token")
    stream: bool = Field(default=False, description="是否流式输出")
    metadata: dict[str, Any] = Field(default_factory=dict, description="扩展信息")
    response_format: dict | None = Field(
        default=None,
        description="模型结构化输出格式约束",
    )
    tools: list[dict] | None = Field(
        default=None,
        description="可用工具列表",
    )
    tool_choice: str | dict | None = Field(
        default=None,
        description="工具调用策略：none / auto / required / 指定工具",
   )

class LLMUsage(BaseModel):
    prompt_tokens: int = Field(default=0, ge=0, description="输入 token")
    completion_tokens: int = Field(default=0, ge=0, description="输出 token")
    total_tokens: int = Field(default=0, ge=0, description="总 token")


class LLMResponse(BaseModel):
    provider: LLMProviderName = Field(..., description="模型供应商")
    model: str = Field(..., description="模型名称")
    content: str = Field(..., description="模型输出内容")
    usage: LLMUsage = Field(default_factory=LLMUsage, description="Token 用量")
    raw_response: dict[str, Any] = Field(default_factory=dict, description="原始响应")
    tool_calls: list[LLMToolCall] = Field(
        default_factory=list,
        description="模型请求调用的工具",
    )
    finish_reason: str | None = Field(
        default=None,
        description="结束原因",
    )

class LLMStreamChunk(BaseModel):
    provider: LLMProviderName = Field(..., description="模型供应商")
    model: str = Field(..., description="模型名称")
    content: str = Field(default="", description="当前增量内容")
    finish_reason: str | None = Field(default=None, description="结束原因")
    usage: LLMUsage | None = Field(default=None, description="Token 用量")
    raw_chunk: dict[str, Any] = Field(default_factory=dict, description="原始 chunk")

