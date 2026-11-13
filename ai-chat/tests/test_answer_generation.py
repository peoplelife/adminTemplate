import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, patch

from app.llm.schemas import LLMMessage, LLMRole


class AnswerGenerationTests(unittest.IsolatedAsyncioTestCase):
    async def test_generate_uses_configured_model_and_passes_messages(self):
        from app.answer_generation.service import AnswerGenerator

        messages = [LLMMessage(role=LLMRole.USER, content="Question")]
        provider = SimpleNamespace(chat=AsyncMock(return_value="answer"))
        with patch("app.answer_generation.service.settings", default_model="deepseek-configured"):
            answer = await AnswerGenerator(provider).generate(messages, max_tokens=123)
        self.assertEqual(answer, "answer")
        request = provider.chat.await_args.args[0]
        self.assertEqual(request.model, "deepseek-configured")
        self.assertEqual(request.messages, messages)
        self.assertEqual(request.max_tokens, 123)
        self.assertFalse(request.stream)
        self.assertIsNone(request.thinking)

    async def test_stream_closes_provider_when_consumer_stops(self):
        from app.answer_generation.service import AnswerGenerator

        closed = []
        requests = []

        async def stream_chat(request):
            requests.append(request)
            try:
                yield "first"
                yield "second"
            finally:
                closed.append(True)

        with patch("app.answer_generation.service.settings", default_model="deepseek-configured"):
            stream = AnswerGenerator(SimpleNamespace(stream_chat=stream_chat)).stream(
                [LLMMessage(role=LLMRole.USER, content="Question")], max_tokens=1500,
            )
            self.assertEqual(await anext(stream), "first")
            await stream.aclose()
        self.assertEqual(closed, [True])
        self.assertEqual(requests[0].model, "deepseek-configured")
        self.assertTrue(requests[0].stream)

    async def test_explicit_model_overrides_default(self):
        from app.answer_generation.service import AnswerGenerator

        provider = SimpleNamespace(chat=AsyncMock(return_value="answer"))
        with patch("app.answer_generation.service.settings", default_model="deepseek-configured"):
            await AnswerGenerator(provider).generate([LLMMessage(role=LLMRole.USER, content="Question")], model="qwen-plus")
        self.assertEqual(provider.chat.await_args.args[0].model, "qwen-plus")

    async def test_generate_passes_agent_tool_contract(self):
        from app.answer_generation.service import AnswerGenerator

        provider = SimpleNamespace(chat=AsyncMock(return_value="answer"))
        tools = [{"type": "function", "function": {"name": "create_page_spec"}}]
        await AnswerGenerator(provider).generate(
            [LLMMessage(role=LLMRole.USER, content="生成页面")],
            tools=tools,
            tool_choice="auto",
            response_format={"type": "json_object"},
            thinking=True,
        )
        request = provider.chat.await_args.args[0]
        self.assertEqual(request.tools, tools)
        self.assertEqual(request.tool_choice, "auto")
        self.assertEqual(request.response_format, {"type": "json_object"})
        self.assertTrue(request.thinking)

    async def test_stream_passes_tool_contract_to_provider(self):
        from app.answer_generation.service import AnswerGenerator

        requests = []

        async def stream_chat(request):
            requests.append(request)
            yield "delta"

        tools = [{"type": "function", "function": {"name": "create_page_spec"}}]
        chunks = [chunk async for chunk in AnswerGenerator(SimpleNamespace(stream_chat=stream_chat)).stream(
            [LLMMessage(role=LLMRole.USER, content="生成页面")],
            tools=tools, tool_choice="auto", response_format={"type": "json_object"}, thinking=True,
        )]
        self.assertEqual(chunks, ["delta"])
        self.assertTrue(requests[0].stream)
        self.assertEqual(requests[0].tools, tools)
        self.assertEqual(requests[0].tool_choice, "auto")
        self.assertEqual(requests[0].response_format, {"type": "json_object"})
        self.assertTrue(requests[0].thinking)

    async def test_provider_preserves_non_streaming_reasoning_and_omits_unspecified_thinking(self):
        import json
        import httpx

        from app.answer_generation.service import AnswerGenerator
        from app.llm.providers.openai_compatible import OpenAICompatibleProvider
        from app.llm.schemas import LLMProviderName

        def respond(request):
            payload = json.loads(request.content)
            self.assertNotIn("thinking", payload)
            return httpx.Response(200, json={
                "model": "deepseek-chat",
                "choices": [{"message": {"content": "答案", "reasoning_content": "模型返回的推理"}, "finish_reason": "stop"}],
            })

        provider = OpenAICompatibleProvider(api_key="test", base_url="https://model.example", default_model="deepseek-chat")
        provider.provider_name = LLMProviderName.DEEPSEEK
        with patch("app.llm.providers.openai_compatible.create_llm_http_client", side_effect=lambda _: httpx.AsyncClient(transport=httpx.MockTransport(respond))):
            result = await AnswerGenerator(provider).generate([LLMMessage(role=LLMRole.USER, content="问题")])
        self.assertEqual(result.content, "答案")
        self.assertEqual(result.reasoning_content, "模型返回的推理")

