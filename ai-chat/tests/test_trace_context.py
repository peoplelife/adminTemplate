import unittest

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.core.middleware import register_middleware
from app.core.request_context import get_trace_id
from app.schemas.response import ApiResponse


class TraceContextTests(unittest.TestCase):
    def setUp(self):
        app = FastAPI()
        register_middleware(app)

        @app.get("/trace")
        async def trace():
            return ApiResponse(data={"context": get_trace_id()})

        self.client = TestClient(app)
        self.addCleanup(self.client.close)

    def test_supplied_trace_matches_context_response_body_and_header(self):
        response = self.client.get("/trace", headers={"X-Trace-Id": "java.trace:123_A-1"})
        self.assertEqual(response.headers["X-Trace-Id"], "java.trace:123_A-1")
        self.assertEqual(response.json()["trace_id"], "java.trace:123_A-1")
        self.assertEqual(response.json()["data"]["context"], "java.trace:123_A-1")

    def test_missing_or_invalid_trace_is_replaced_with_safe_id(self):
        for supplied in (None, "", " ", "a" * 129, "unsafe\nvalue", "bad/value", "é"):
            with self.subTest(supplied=supplied):
                headers = {"X-Trace-Id": supplied} if supplied is not None else {}
                # TestClient requires ASCII headers; latin-1 bytes model ASGI input.
                headers = {k: v.encode("latin-1") for k, v in headers.items()}
                response = self.client.get("/trace", headers=headers)
                trace_id = response.headers["X-Trace-Id"]
                self.assertRegex(trace_id, r"^[A-Za-z0-9._:-]{1,128}$")
                self.assertNotEqual(trace_id, supplied)
                self.assertEqual(response.json()["trace_id"], trace_id)


if __name__ == "__main__":
    unittest.main()
