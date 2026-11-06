import json
import unittest

import httpx

from app.rag.embeddings import EmbeddingServiceError, OpenAICompatibleEmbeddingClient


class RagEmbeddingClientTests(unittest.IsolatedAsyncioTestCase):
    def client_for(self, handler, **overrides):
        http_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
        self.addAsyncCleanup(http_client.aclose)
        options = {
            "base_url": "https://embedding.example/v1",
            "model": "bge-m3",
            "api_key": "private-test-key",
            "client": http_client,
            "batch_size": 2,
        }
        options.update(overrides)
        return OpenAICompatibleEmbeddingClient(**options)

    async def test_batches_requests_and_restores_provider_order(self):
        requests = []

        def handler(request):
            requests.append(json.loads(request.content))
            count = len(requests[-1]["input"])
            data = [
                {"index": index, "embedding": [float(index + 1)] + [0.0] * 1023}
                for index in reversed(range(count))
            ]
            return httpx.Response(200, json={"data": data})

        client = self.client_for(handler)
        vectors = await client.embed_documents(["first", "second", "third"])

        self.assertEqual([len(request["input"]) for request in requests], [2, 1])
        self.assertEqual([vector[0] for vector in vectors], [1.0, 2.0, 1.0])

    async def test_sends_configured_model_and_optional_bearer_token(self):
        seen = {}

        def handler(request):
            seen["authorization"] = request.headers.get("Authorization")
            seen["path"] = request.url.path
            seen["model"] = json.loads(request.content)["model"]
            return httpx.Response(
                200,
                json={"data": [{"index": 0, "embedding": [1.0] + [0.0] * 1023}]},
            )

        client = self.client_for(handler)
        await client.embed_query("question")

        self.assertEqual(seen, {
            "authorization": "Bearer private-test-key",
            "path": "/v1/embeddings",
            "model": "bge-m3",
        })

    async def test_rejects_wrong_dimension_non_finite_and_zero_vectors(self):
        responses = (
            {"data": [{"index": 0, "embedding": [1.0]}]},
            {"data": [{"index": 0, "embedding": [float("nan")] + [0.0] * 1023}]},
            {"data": [{"index": 0, "embedding": [0.0] * 1024}]},
        )
        for body in responses:
            with self.subTest(body_size=len(body["data"][0]["embedding"])):
                client = self.client_for(lambda request, body=body: httpx.Response(200, json=body))
                with self.assertRaises(EmbeddingServiceError):
                    await client.embed_query("question")

    async def test_maps_http_failure_without_exposing_provider_body(self):
        client = self.client_for(
            lambda request: httpx.Response(401, text="private-test-key is invalid")
        )

        with self.assertRaises(EmbeddingServiceError) as raised:
            await client.embed_query("question")

        self.assertNotIn("private-test-key", str(raised.exception))

    async def test_empty_input_does_not_send_a_request(self):
        def fail_if_called(request):
            raise AssertionError("empty input must not call the provider")

        client = self.client_for(fail_if_called)
        self.assertEqual(await client.embed_documents([]), [])


if __name__ == "__main__":
    unittest.main()
