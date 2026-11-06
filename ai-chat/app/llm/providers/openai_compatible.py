import json
from collections.abc import AsyncGenerator

import httpx


from app.llm.base import BaseLLMProvider
from app.llm.errors import (
    LLMProviderConfigError,
    LLMProviderError,
    LLMProviderResponseParseError,
    LLMProviderTimeoutError,
)
from app.llm.http_client import create_llm_http_client
from app.llm.schemas import (
    LLMProviderName,
    LLMRequest,
    LLMResponse,
    LLMStreamChunk,
    LLMToolCall,
    LLMUsage,
)




class OpenAICompatibleProvider(BaseLLMProvider):
    provider_name: LLMProviderName

    def __init__(
        self,
        api_key: str,
        base_url: str,
        default_model: str,
        timeout_seconds: float = 60,
    ):
        if not api_key:
            raise LLMProviderConfigError(
                message="LLM API Key 未配置",
                provider=self.provider_name.value,
            )

        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.default_model = default_model
        self.timeout_seconds = timeout_seconds

    async def chat(self, request: LLMRequest) -> LLMResponse:
        payload = self._build_payload(request=request, stream=False)
        url = f"{self.base_url}/chat/completions"

        

        try:
            async with create_llm_http_client(self.timeout_seconds) as client:
                response = await client.post(
                    url,
                    headers=self._headers(),
                    json=payload,
                )

                response.raise_for_status()

        except httpx.TimeoutException as exc:
            
            raise LLMProviderTimeoutError(
                message="大模型调用超时",
                provider=self.provider_name.value,
            ) from exc

        except httpx.HTTPStatusError as exc:
            
            raise LLMProviderError(
                message="大模型 HTTP 调用失败",
                provider=self.provider_name.value,
                status_code=exc.response.status_code,
                raw_error=exc.response.text,
            ) from exc

        except httpx.RequestError as exc:
            
            raise LLMProviderError(
                message="大模型网络请求失败",
                provider=self.provider_name.value,
                raw_error=repr(exc),
            ) from exc

        data = response.json()

        try:
            llm_response = self._parse_chat_response(data)
        except Exception as exc:
            
            raise LLMProviderResponseParseError(
                message="大模型响应解析失败",
                provider=self.provider_name.value,
                raw_error=json.dumps(data, ensure_ascii=False),
            ) from exc

        

        return llm_response

    async def stream_chat(
        self,
        request: LLMRequest,
    ) -> AsyncGenerator[LLMStreamChunk, None]:
        payload = self._build_payload(request=request, stream=True)
        url = f"{self.base_url}/chat/completions"

        

        try:
            async with create_llm_http_client(self.timeout_seconds) as client:
                async with client.stream(
                    "POST",
                    url,
                    headers=self._headers(),
                    json=payload,
                ) as response:
                    response.raise_for_status()

                    async for line in response.aiter_lines():
                        if not line:
                            continue

                        if not line.startswith("data:"):
                            continue

                        data = line.removeprefix("data:").strip()

                        if data == "[DONE]":
                            break

                        chunk_data = json.loads(data)
                        chunk = self._parse_stream_chunk(chunk_data)

                        yield chunk

        except httpx.TimeoutException as exc:
            
            raise LLMProviderTimeoutError(
                message="大模型流式调用超时",
                provider=self.provider_name.value,
            ) from exc

        except httpx.HTTPStatusError as exc:
            
            raise LLMProviderError(
                message="大模型流式 HTTP 调用失败",
                provider=self.provider_name.value,
                status_code=exc.response.status_code,
                raw_error=exc.response.text,
            ) from exc

        except httpx.RequestError as exc:
            
            raise LLMProviderError(
                message="大模型流式网络请求失败",
                provider=self.provider_name.value,
                raw_error=repr(exc),
            ) from exc

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }

    def _build_payload(
        self,
        request: LLMRequest,
        stream: bool,
    ) -> dict:
        messages=[]
        for message in request.messages:
            item={
                'role':message.role.value,
                'content':message.content
            }

            if message.tool_calls:
                item['tool_calls']=message.tool_calls

            if message.tool_call_id:
                item['tool_call_id']=message.tool_call_id

            messages.append(item)

        payload = {
            "model": request.model or self.default_model,
            "messages": messages,
            "temperature": request.temperature,
            "stream": stream,
        }

        if request.max_tokens is not None:
            payload["max_tokens"] = request.max_tokens
            
        if request.response_format is not None:
            payload["response_format"] = request.response_format

        if request.tools:
           payload["tools"] = request.tools

        if request.tool_choice is not None:
           payload["tool_choice"] = request.tool_choice

        return payload

    def _parse_chat_response(self, data: dict) -> LLMResponse:
        choice = data["choices"][0]
        message = choice["message"]

        usage_data = data.get("usage") or {}

        usage = LLMUsage(
            prompt_tokens=usage_data.get("prompt_tokens", 0),
            completion_tokens=usage_data.get("completion_tokens", 0),
            total_tokens=usage_data.get("total_tokens", 0),
        )
        tool_calls = self._parse_tool_calls(message)

        return LLMResponse(
            provider=self.provider_name,
            model=data.get("model", self.default_model),
            content=message.get("content", ""),
            usage=usage,
            tool_calls=tool_calls,
            finish_reason=choice.get("finish_reason"),
            raw_response=data,
        )
    def _parse_tool_calls(self, message: dict) -> list[LLMToolCall]:
        raw_tool_calls = message.get("tool_calls") or []
        parsed_tool_calls: list[LLMToolCall] = []

        for item in raw_tool_calls:
            function = item.get("function") or {}
            raw_arguments = function.get("arguments") or "{}"

            try:
                arguments = json.loads(raw_arguments)
            except json.JSONDecodeError:
                arguments = {}

            parsed_tool_calls.append(
                LLMToolCall(
                    id=item.get("id", ""),
                    name=function.get("name", ""),
                    arguments=arguments,
                    raw_arguments=raw_arguments,
                )
            )

        return parsed_tool_calls

    def _parse_stream_chunk(self, data: dict) -> LLMStreamChunk:
        choice = data["choices"][0]
        delta = choice.get("delta") or {}
        usage_data = data.get("usage")

        usage = None

        if usage_data:
            usage = LLMUsage(
                prompt_tokens=usage_data.get("prompt_tokens", 0),
                completion_tokens=usage_data.get("completion_tokens", 0),
                total_tokens=usage_data.get("total_tokens", 0),
            )

        return LLMStreamChunk(
            provider=self.provider_name,
            model=data.get("model", self.default_model),
            content=delta.get("content") or "",
            finish_reason=choice.get("finish_reason"),
            usage=usage,
            raw_chunk=data,
        )