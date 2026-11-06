import unittest

from fastapi import HTTPException

from app.core.config import Settings
from app.dependencies.rag_auth import get_rag_principal


class RagAuthTests(unittest.IsolatedAsyncioTestCase):
    async def call_dependency(self, service_token, user_id, settings):
        return await get_rag_principal(
            x_service_token=service_token,
            x_user_id=user_id,
            settings=settings,
        )

    async def test_accepts_service_token_and_stable_user_id(self):
        principal = await self.call_dependency(
            "private-service-secret",
            "42",
            Settings(rag_service_token="private-service-secret", _env_file=None),
        )

        self.assertEqual(principal.owner_id, "42")

    async def test_fails_closed_when_service_token_is_not_configured(self):
        with self.assertRaises(HTTPException) as raised:
            await self.call_dependency(
                "anything", "42", Settings(rag_service_token="", _env_file=None)
            )

        self.assertEqual(raised.exception.status_code, 503)

    async def test_fails_closed_when_service_token_contains_only_whitespace(self):
        with self.assertRaises(HTTPException) as raised:
            await self.call_dependency(" ", "42", Settings(rag_service_token=" ", _env_file=None))

        self.assertEqual(raised.exception.status_code, 503)

    async def test_accepts_non_ascii_service_token(self):
        secret = "rag-服务-token"
        principal = await self.call_dependency(secret, "42", Settings(rag_service_token=secret, _env_file=None))

        self.assertEqual(principal.owner_id, "42")

    async def test_rejects_missing_service_token(self):
        with self.assertRaises(HTTPException) as raised:
            await self.call_dependency(
                None,
                "42",
                Settings(rag_service_token="private-service-secret", _env_file=None),
            )

        self.assertEqual(raised.exception.status_code, 401)

    async def test_rejects_invalid_service_token(self):
        with self.assertRaises(HTTPException) as raised:
            await self.call_dependency(
                "wrong-secret",
                "42",
                Settings(rag_service_token="private-service-secret", _env_file=None),
            )

        self.assertEqual(raised.exception.status_code, 403)

    async def test_rejects_missing_or_invalid_user_id(self):
        settings = Settings(rag_service_token="private-service-secret", _env_file=None)
        for user_id in (None, "", " 42", "x" * 65, "42\n"):
            with self.subTest(user_id=user_id), self.assertRaises(HTTPException):
                await self.call_dependency("private-service-secret", user_id, settings)


if __name__ == "__main__":
    unittest.main()
