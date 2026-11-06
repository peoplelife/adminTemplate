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

