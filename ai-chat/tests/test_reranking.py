import unittest
import httpx

from app.rag.reranking.client import RerankClient, RerankServiceError


class RerankClientTests(unittest.IsolatedAsyncioTestCase):
    async def test_returns_api_order_and_scores(self):
        transport = httpx.MockTransport(lambda request: httpx.Response(200, json={
            "results": [
                {"index": 1, "relevance_score": 0.91},
                {"index": 0, "relevance_score": 0.22},
            ],
        }))
        client = RerankClient("https://api.siliconflow.cn/v1", "BAAI/bge-reranker-v2-m3", "key", client=httpx.AsyncClient(transport=transport))
        try:
            self.assertEqual(
                await client.rerank("leave", ["annual leave", "unrelated"]),
                [(1, 0.91), (0, 0.22)],
            )
        finally:
            await client.aclose()

    async def test_rejects_invalid_response(self):
        transport = httpx.MockTransport(lambda request: httpx.Response(200, json={"results": []}))
        client = RerankClient("https://api.siliconflow.cn/v1", "model", client=httpx.AsyncClient(transport=transport))
        try:
            with self.assertRaises(RerankServiceError):
                await client.rerank("leave", ["annual leave"])
        finally:
            await client.aclose()

    async def test_hides_http_error_response_content(self):
        transport = httpx.MockTransport(lambda request: httpx.Response(401, text="private service details"))
        client = RerankClient("https://api.siliconflow.cn/v1", "model", client=httpx.AsyncClient(transport=transport))
        try:
            with self.assertRaises(RerankServiceError) as raised:
                await client.rerank("leave", ["annual leave"])
            self.assertNotIn("private", str(raised.exception))
        finally:
            await client.aclose()
