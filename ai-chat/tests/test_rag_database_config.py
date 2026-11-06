import unittest
from app.core.config import Settings


class RagDatabaseConfigTests(unittest.TestCase):
    def test_shared_settings_contains_rag_configuration(self):
        self.assertIn("rag_service_token", Settings.model_fields)
        self.assertIn("database_url", Settings.model_fields)


if __name__ == "__main__":
    unittest.main()
