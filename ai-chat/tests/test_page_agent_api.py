import unittest

from fastapi.testclient import TestClient

from app.api.v1.page_agent import get_page_generation_service
from app.core.config import get_settings
from app.dependencies.redis import get_redis_client
from app.main import create_app
from app.page_generation.agent import PageAgent
from app.schemas.page_agent import PageAgentWorkspaceResponse
from app.services.page_agent_service import PageGenerationService
from tests.test_page_generation import FakeGenerator


class FakeRedis:
    def __init__(self):
        self.values = {}

    async def get(self, key):
        return self.values.get(key)

    async def set(self, key, value, ex=None):
        self.values[key] = value
        return True

    async def delete(self, *keys):
        count = sum(key in self.values for key in keys)
        for key in keys:
            self.values.pop(key, None)
        return count

    async def eval(self, script, key_count, *args):
        generation_key, workspace_key, token, value, ttl = args
        if self.values.get(generation_key) == token:
            return int(await self.set(workspace_key, value, ex=ttl))
        return 0


class PageAgentApiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.app = create_app()
        cls.redis = FakeRedis()
        cls.app.dependency_overrides[get_settings] = lambda: get_settings().model_copy(
            update={"rag_service_token": "test-service-token"}
        )
        cls.app.dependency_overrides[get_redis_client] = lambda: cls.redis
        cls.app.dependency_overrides[get_page_generation_service] = lambda: PageGenerationService(
            agent=PageAgent(generator=FakeGenerator())
        )
        cls.client = TestClient(cls.app)

    @classmethod
    def tearDownClass(cls):
        cls.client.close()
        cls.app.dependency_overrides.clear()

    @staticmethod
    def headers():
        return {
            "X-Service-Token": "test-service-token",
            "X-User-Id": "42",
            "X-Trace-Id": "page-agent-trace",
        }

    def test_workspace_requires_service_identity(self):
        self.redis.values.clear()
        response = self.client.get("/api/v1/ai/page-agent/workspace", headers=self.headers())
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["data"], {"messages": [], "pageSpec": None, "preview": None})

    def test_stream_uses_sse_contract(self):
        response = self.client.post(
            "/api/v1/ai/page-agent/messages/stream",
            headers=self.headers(),
            json={"content": "生成客户页面"},
        )
        self.assertEqual(response.status_code, 200)
        self.assertIn("event: status", response.text)
        self.assertIn("event: page_spec", response.text)
        self.assertIn('"formColumns": 2', response.text)
        self.assertIn("event: done", response.text)

    def test_workspace_restores_generated_page_and_reset_clears_only_the_owner(self):
        self.client.post("/api/v1/ai/page-agent/messages/stream", headers=self.headers(), json={"content": "生成客户页面"})
        response = self.client.get("/api/v1/ai/page-agent/workspace", headers=self.headers())
        self.assertEqual(response.json()["data"]["pageSpec"]["title"], "客户管理")
        other = {**self.headers(), "X-User-Id": "43"}
        self.assertIsNone(self.client.get("/api/v1/ai/page-agent/workspace", headers=other).json()["data"]["pageSpec"])
        reset = self.client.post("/api/v1/ai/page-agent/reset", headers=self.headers())
        self.assertEqual(reset.json()["data"], {"messages": [], "pageSpec": None, "preview": None})

    def test_all_routes_reject_missing_internal_credentials(self):
        for method, path in [("get", "workspace"), ("post", "reset"), ("post", "messages/stream")]:
            with self.subTest(path=path):
                response = getattr(self.client, method)(f"/api/v1/ai/page-agent/{path}")
                self.assertEqual(response.status_code, 401)

    def test_stream_rejects_invalid_credentials_and_blank_or_oversized_messages(self):
        response = self.client.post(
            "/api/v1/ai/page-agent/messages/stream", headers={**self.headers(), "X-Service-Token": "wrong"},
            json={"content": "生成客户页面"},
        )
        self.assertEqual(response.status_code, 403)
        for content in ["   ", "x" * 4001]:
            response = self.client.post("/api/v1/ai/page-agent/messages/stream", headers=self.headers(), json={"content": content})
            self.assertEqual(response.status_code, 422)


if __name__ == "__main__":
    unittest.main()
