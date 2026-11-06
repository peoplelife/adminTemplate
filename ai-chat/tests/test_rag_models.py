import unittest

from app.db.base import Base
from app.models.rag import Chunk, Document, KnowledgeBase, RagMessage


class RagModelTests(unittest.TestCase):
    def test_rag_models_use_public_metadata_and_define_expected_constraints(self):
        expected_tables = {
            "knowledge_bases",
            "documents",
            "chunks",
            "conversations",
            "messages",
            "message_sources",
        }

        self.assertEqual(
            {
                table.name
                for table in Base.metadata.tables.values()
                if table.name in expected_tables
            },
            expected_tables,
        )
        self.assertTrue(
            all(
                table.schema is None
                for table in Base.metadata.tables.values()
                if table.name in expected_tables
            )
        )
        self.assertIn("knowledge_bases", Base.metadata.tables)
        self.assertEqual(KnowledgeBase.__table__.c.owner_id.type.length, 64)
        self.assertEqual(KnowledgeBase.__table__.c.embedding_dimension.type.python_type, int)
        self.assertEqual(Chunk.__table__.c.embedding.type.dim, 1024)
        self.assertTrue(
            any(
                index.name == "uq_documents_kb_file_hash_live"
                and index.unique
                and index.dialect_options["postgresql"]["where"] is not None
                for index in Document.__table__.indexes
            )
        )
        self.assertTrue(
            any(
                index.name == "uq_messages_active_generation"
                and index.unique
                and index.dialect_options["postgresql"]["where"] is not None
                for index in RagMessage.__table__.indexes
            )
        )


if __name__ == "__main__":
    unittest.main()
