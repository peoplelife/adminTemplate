from enum import Enum

from pydantic import BaseModel,  Field


class PromptRole(str,Enum):
    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"

class PromptScenario(str,Enum):
    GENERAL_CHAT = "general_chat"
    ECOMMERCE_CUSTOMER_SERVICE = "ecommerce_customer_service"
    RAG_QA = "rag_qa"
    RAG_EVALUATION = "rag_evaluation"
    AGENT_PLANNER = "agent_planner"
    PAGE_GENERATION = "page_generation"

class PromptMessageTemplate(BaseModel):
    role: PromptRole = Field(..., description="消息角色")
    template: str = Field(..., min_length=1, description="消息模板")

class PromptTemplate(BaseModel):
    prompt_id: str = Field(..., description="Prompt 模板 ID")
    name: str = Field(..., description="Prompt 模板名称")
    scenario: PromptScenario = Field(..., description="Prompt 模板场景")
    version: str = Field(..., description="Prompt 模板版本")
    messages: list[PromptMessageTemplate] = Field(
        ...,
        min_length=1,
        description="消息模板列表",
    )
    required_variables: list[str] = Field(
        default_factory=list,
        description="必填变量",
    )
