from collections.abc import AsyncGenerator

from app.core.config import settings
from app.llm.base import BaseLLMProvider
from app.llm.factory import LLMProviderFactory, infer_provider_from_model
from app.llm.schemas import LLMMessage, LLMRequest, LLMResponse, LLMStreamChunk


class AnswerGenerator:
    def __init__(self, provider: BaseLLMProvider | None = None):
        self.provider = provider

    async def generate(
        self, messages: list[LLMMessage], *, model: str | None = None, max_tokens: int | None = None,
    ) -> LLMResponse:
        request = LLMRequest(model=model or settings.default_model, messages=messages, max_tokens=max_tokens)
        provider = self.provider or LLMProviderFactory.create(infer_provider_from_model(request.model))
        return await provider.chat(request)

    async def stream(
        self, messages: list[LLMMessage], *, model: str | None = None, max_tokens: int | None = None,
    ) -> AsyncGenerator[LLMStreamChunk, None]:
        request = LLMRequest(model=model or settings.default_model, messages=messages, max_tokens=max_tokens, stream=True)
        provider = self.provider or LLMProviderFactory.create(infer_provider_from_model(request.model))
        stream = provider.stream_chat(request)
        try:
            async for chunk in stream:
                yield chunk
        finally:
            await stream.aclose()


answer_generator = AnswerGenerator()
