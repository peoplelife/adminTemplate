import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock

from pydantic import ValidationError
from sqlalchemy.dialects import postgresql

from app.dependencies.pagination import Page, PageParams
from app.repositories.rag_repository import RagRepository


class PaginationTests(unittest.TestCase):
    def test_page_bounds_and_response_contract(self):
        params = PageParams()
        self.assertEqual((params.page, params.page_size), (1, 20))
        for values in ({"page": 0}, {"page_size": 0}, {"page_size": 101}):
            with self.subTest(values=values), self.assertRaises(ValidationError):
                PageParams(**values)
        page = Page[int](items=[3], total=5, page=2, page_size=2)
        self.assertEqual(page.model_dump(by_alias=True), {
            "items": [3], "total": 5, "page": 2, "pageSize": 2,
        })


class PaginationQueryTests(unittest.IsolatedAsyncioTestCase):
    async def test_page_two_uses_offset_and_owner_filter_for_data_and_total(self):
        from uuid import UUID
        kb_id = UUID("28927c14-4dcf-49db-9a91-d81629cb2392")
        repository = RagRepository()
        for method, extra in (
            (repository.list_knowledge_bases, {}),
            (repository.list_documents, {"knowledge_base_id": kb_id}),
            (repository.list_conversations, {"mode": "knowledge", "knowledge_base_id": kb_id}),
        ):
            with self.subTest(method=method.__name__):
                rows = Mock()
                item = SimpleNamespace(id=kb_id)
                rows.all.return_value = [(item, 2, 1)] if method.__name__ == "list_knowledge_bases" else [(item,)]
                rows.scalars.return_value.all.return_value = [item]
                session = SimpleNamespace(execute=AsyncMock(side_effect=[
                    SimpleNamespace(scalar_one=lambda: 27), rows,
                ]))
                items, total = await method(session, owner_id="42", page=2, page_size=10, **extra)
                self.assertEqual(total, 27)
                self.assertEqual(items, [(item, 2, 1)] if method.__name__ == "list_knowledge_bases" else [item])
                statements = [call.args[0].compile(dialect=postgresql.dialect())
                              for call in session.execute.await_args_list]
                self.assertTrue(all("owner_id" in str(sql) for sql in statements))
                self.assertTrue(all("42" in sql.params.values() for sql in statements))
                self.assertNotIn("OFFSET", str(statements[0]))
                self.assertIn("OFFSET", str(statements[1]))
                self.assertEqual(list(statements[1].params.values()).count(10), 2)
                if extra:
                    self.assertTrue(all(kb_id in sql.params.values() for sql in statements))
