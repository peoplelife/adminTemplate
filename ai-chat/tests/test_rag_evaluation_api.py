import unittest
from datetime import UTC, datetime
from types import SimpleNamespace
from unittest.mock import AsyncMock
from uuid import uuid4

from fastapi.testclient import TestClient
from sqlalchemy.dialects import postgresql

from app.api.v1.rag_evaluation import get_evaluation_service
from app.core.config import get_settings
from app.dependencies.database import get_db_session
from app.main import create_app
from app.repositories.rag_evaluation_repository import RagEvaluationRepository


class EvaluationApiTests(unittest.TestCase):
    def setUp(self):
        self.app = create_app()
        self.service = SimpleNamespace(create_dataset=AsyncMock(), save_case=AsyncMock(),
            list_datasets=AsyncMock(return_value={"items": [], "total": 0, "page": 2, "page_size": 10}),
            delete_dataset=AsyncMock(), run=AsyncMock())
        async def database():
            yield SimpleNamespace()
        self.app.dependency_overrides[get_db_session] = database
        self.app.dependency_overrides[get_evaluation_service] = lambda: self.service
        self.app.dependency_overrides[get_settings] = lambda: get_settings().model_copy(update={"rag_service_token": "test-token"})
        self.client = TestClient(self.app)
        self.addCleanup(self.client.close)
        self.headers = {"X-Service-Token": "test-token", "X-User-Id": "42"}
        self.path = "/api/v1/rag/evaluation-datasets"

    def test_requires_trusted_identity_and_passes_owner_and_page(self):
        self.assertEqual(self.client.get(self.path).status_code, 401)
        self.service.list_datasets.assert_not_awaited()
        response = self.client.get(self.path + "?page=2&page_size=10", headers=self.headers)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"], {"items": [], "total": 0, "page": 2, "pageSize": 10})
        values = self.service.list_datasets.await_args.kwargs
        self.assertEqual((values["owner_id"], values["page"], values["page_size"]), ("42", 2, 10))

    def test_invalid_case_and_top_k_rejected_at_boundary(self):
        base = self.path + "/" + str(uuid4())
        response = self.client.post(base + "/cases", headers=self.headers, json={"question": "test?"})
        self.assertEqual(response.status_code, 422)
        self.service.save_case.assert_not_awaited()
        response = self.client.post(base + "/runs", headers=self.headers, json={"topK": 21})
        self.assertEqual(response.status_code, 422)
        self.service.run.assert_not_awaited()

    def test_create_and_delete_have_explicit_contracts(self):
        kb_id, dataset_id = uuid4(), uuid4()
        self.service.create_dataset.return_value = dict(id=dataset_id, knowledge_base_id=kb_id,
            name="Policy", description="", created_at=datetime.now(UTC))
        response = self.client.post(self.path, headers=self.headers, json={"name": "Policy", "knowledgeBaseId": str(kb_id)})
        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["data"]["knowledgeBaseId"], str(kb_id))
        response = self.client.delete(self.path + "/" + str(dataset_id), headers=self.headers)
        self.assertEqual(response.status_code, 204)
        self.assertEqual(response.content, b"")


class EvaluationIsolationQueryTests(unittest.IsolatedAsyncioTestCase):
    async def test_dataset_query_filters_owner_and_id(self):
        dataset_id = uuid4()
        session = SimpleNamespace(execute=AsyncMock(return_value=SimpleNamespace(scalar_one_or_none=lambda: None)))
        await RagEvaluationRepository().dataset(session, owner_id="owner", dataset_id=dataset_id)
        query = session.execute.await_args.args[0].compile(dialect=postgresql.dialect())
        self.assertIn("owner", query.params.values())
        self.assertIn(dataset_id, query.params.values())

    async def test_evidence_documents_query_enforces_owner_and_knowledge_base(self):
        kb_id = uuid4()
        session = SimpleNamespace(execute=AsyncMock(return_value=SimpleNamespace(scalars=lambda: [])))
        await RagEvaluationRepository().documents(session, owner_id="owner", knowledge_base_id=kb_id)
        query = session.execute.await_args.args[0].compile(dialect=postgresql.dialect())
        self.assertIn("owner", query.params.values())
        self.assertIn(kb_id, query.params.values())
        self.assertIn("JOIN knowledge_bases", str(query))
