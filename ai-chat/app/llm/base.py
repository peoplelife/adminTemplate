from abc import ABC, abstractmethod
from collections.abc import AsyncGenerator

from app.llm.schemas import LLMRequest, LLMResponse, LLMStreamChunk


class BaseLLMProvider(ABC):
    @abstractmethod
    async def chat(self, request: LLMRequest) -> LLMResponse:
        pass

    @abstractmethod
    async def stream_chat(
        self,
        request: LLMRequest,
    ) -> AsyncGenerator[LLMStreamChunk, None]:
        pass