"""Opt-in checks against DATABASE_URL; retain synthetic verification conversations."""
import asyncio
import os
import unittest
from types import SimpleNamespace
from uuid import uuid4

from sqlalchemy import select

from app.core.config import settings
from app.core.exceptions import AppException
from app.db.session import AsyncSessionLocal, engine
from app.models.rag import RagMessage
from app.repositories.rag_repository import RagRepository
from app.services.rag_conversation_service import RagConversationService


class Provider:
    def __init__(self, content="partial", fail=False):
        self.content = content
        self.fail = fail

    async def stream_chat(self, _):
        yield SimpleNamespace(content=self.content, finish_reason=None, usage=None)
        if self.fail:
            raise RuntimeError("synthetic failure")


@unittest.skipUnless(os.getenv("RUN_RAG_POSTGRES_CHECKS") == "1", "requires explicit PostgreSQL opt-in")
class RagConversationPostgresTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.factory = AsyncSessionLocal
        self.repository = RagRepository()
        self.owner = "verify-" + uuid4().hex
        async with self.factory() as session:
            conversation = await self.repository.create_conversation(
                session, owner_id=self.owner, title="RAG reliability verification",
                knowledge_base_id=None, model=settings.default_model)
            await session.commit()
            self.conversation_id = conversation.id

    async def asyncTearDown(self):
        await engine.dispose()

    async def start_stream(self, session, provider=None):
        service = RagConversationService(repository=self.repository, llm_provider=provider or Provider())
        return await service.stream_message(
            session, owner_id=self.owner, conversation_id=self.conversation_id,
            request=SimpleNamespace(client_request_id=uuid4(), content="Synthetic verification"))

    async def assistant(self):
        async with self.factory() as session:
            return (await session.execute(select(RagMessage).where(
                RagMessage.conversation_id == self.conversation_id,
                RagMessage.role == "assistant").order_by(RagMessage.seq.desc()).limit(1))).scalar_one()

    async def test_done_is_visible_only_after_final_commit(self):
        async with self.factory() as session:
            stream = await self.start_stream(session)
            async for event in stream:
                if event.startswith("event: done"):
                    message = await self.assistant()
                    self.assertEqual((message.status, message.content), ("completed", "partial"))

    async def test_concurrent_requests_allow_only_one_generation(self):
        rejected = asyncio.Event()

        async def attempt():
            async with self.factory() as session:
                try:
                    stream = await self.start_stream(session)
                except AppException as exc:
                    rejected.set()
                    return exc.code
                await asyncio.wait_for(rejected.wait(), timeout=3)
                output = [event async for event in stream]
                self.assertTrue(output[-1].startswith("event: done"))
                return 0

        self.assertEqual(sorted(await asyncio.gather(attempt(), attempt())), [0, 160123])
        async with self.factory() as session:
            messages, _ = await self.repository.list_conversation_messages(
                session, owner_id=self.owner, conversation_id=self.conversation_id)
            self.assertEqual(len(messages), 2)

    async def test_failure_after_rollback_preserves_partial_answer(self):
        async with self.factory() as session:
            stream = await self.start_stream(session, Provider(fail=True))
            output = [event async for event in stream]
        self.assertTrue(output[-1].startswith("event: error"))
        message = await self.assistant()
        self.assertEqual((message.status, message.content), ("failed", "partial"))

    async def test_disconnected_consumer_preserves_partial_answer(self):
        async with self.factory() as session:
            stream = await self.start_stream(session)
            async for event in stream:
                if event.startswith("event: message"):
                    break
            await stream.aclose()
        message = await self.assistant()
        self.assertEqual((message.status, message.content), ("cancelled", "partial"))

    async def test_concurrent_delete_blocks_stale_generation_completion(self):
        async with self.factory() as session:
            stream = await self.start_stream(session)
            async for event in stream:
                if event.startswith("event: message"):
                    break
            async with self.factory() as deletion:
                await RagConversationService().delete_conversation(
                    deletion, owner_id=self.owner, conversation_id=self.conversation_id)
            remaining = [event async for event in stream]
        self.assertFalse(any(event.startswith("event: done") for event in remaining))
        self.assertEqual((await self.assistant()).status, "cancelled")
