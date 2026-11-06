import unittest
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import AsyncMock
from uuid import uuid4

from app.core.exceptions import AppException
from app.schemas.rag_management import KnowledgeBaseCreateRequest
from app.services.rag_management_service import RagManagementService


def knowledge_base(**overrides):
    now = datetime.now(UTC)
    values = {
        "id": uuid4(),
        "owner_id": "42",
        "name": "Employee handbook",
        "description": "Internal policies",
        "status": "active",
        "embedding_model": "BAAI/bge-m3",
        "embedding_dimension": 1024,
        "chunk_size": 600,
        "chunk_overlap": 100,
        "created_at": now,
        "updated_at": now,
    }
    values.update(overrides)
    return SimpleNamespace(**values)


class RagManagementServiceTests(unittest.IsolatedAsyncioTestCase):
    def setUp(self):
        self.repository = SimpleNamespace(
            create_knowledge_base=AsyncMock(),
            get_knowledge_base=AsyncMock(),
            list_knowledge_bases=AsyncMock(),
        )
        self.service = RagManagementService(
            repository=self.repository,
            embedding_model="BAAI/bge-m3",
        )
        self.session = SimpleNamespace(
            commit=AsyncMock(),
            rollback=AsyncMock(),
        )

    async def test_create_trims_fields_and_scopes_owner(self):
        model = knowledge_base(name="Employee handbook", description="Internal policies")
        self.repository.create_knowledge_base.return_value = model

        result = await self.service.create_knowledge_base(
            self.session,
            owner_id="42",
            request=KnowledgeBaseCreateRequest(
                name="  Employee handbook  ",
                description="  Internal policies  ",
            ),
        )

        call = self.repository.create_knowledge_base.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["name"], "Employee handbook")
        self.assertEqual(call["description"], "Internal policies")
        self.assertEqual(call["embedding_model"], "BAAI/bge-m3")
        self.assertEqual(result.id, model.id)
        self.assertEqual(result.embedding_dimension, 1024)
        self.session.commit.assert_awaited_once()

    async def test_get_uses_owner_scope_and_hides_missing_resource(self):
        knowledge_base_id = uuid4()
        self.repository.get_knowledge_base.return_value = None

        with self.assertRaises(AppException) as raised:
            await self.service.get_knowledge_base(
                self.session,
                owner_id="other-user",
                knowledge_base_id=knowledge_base_id,
            )

        self.repository.get_knowledge_base.assert_awaited_once_with(
            self.session,
            owner_id="other-user",
            knowledge_base_id=knowledge_base_id,
        )
        self.assertEqual(raised.exception.code, 160101)
        self.assertEqual(raised.exception.status_code, 200)

    async def test_list_returns_counts_and_page_number(self):
        first = knowledge_base()
        second = knowledge_base(name="Product manual")
        self.repository.list_knowledge_bases.return_value = ([(first, 2, 1)], 2)

        result = await self.service.list_knowledge_bases(
            self.session,
            owner_id="42",
            page=2,
            page_size=1,
        )

        self.assertEqual(len(result.items), 1)
        self.assertEqual(result.items[0].document_count, 2)
        self.assertEqual(result.items[0].ready_document_count, 1)
        self.assertEqual(result.total, 2)
        self.assertEqual(result.page, 2)
        call = self.repository.list_knowledge_bases.await_args.kwargs
        self.assertEqual(call["owner_id"], "42")
        self.assertEqual(call["page_size"], 1)
        self.assertEqual(call["page"], 2)


if __name__ == "__main__":
    unittest.main()
