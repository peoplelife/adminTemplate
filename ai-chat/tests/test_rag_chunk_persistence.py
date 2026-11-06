import unittest
from types import SimpleNamespace
from unittest.mock import AsyncMock, Mock
from uuid import uuid4

from app.document_parsing.service import parse_document
from app.document_chunking.schemas import ChunkConfig
from app.document_chunking.service import split_document
from app.repositories.rag_repository import RagRepository


class RagChunkPersistenceTests(unittest.IsolatedAsyncioTestCase):
    async def test_indexes_source_locations_configuration_and_versions(self):
        parsed = parse_document("policy.md", b"# Leave\nAnnual leave rules.\n# Requests\nSubmit here.")
        result = split_document(parsed, ChunkConfig(chunk_size=200, chunk_overlap=20))
        document = SimpleNamespace(id=uuid4(), knowledge_base_id=uuid4())
        session = SimpleNamespace(execute=AsyncMock(), add=Mock(), flush=AsyncMock())

        await RagRepository().replace_document_chunks(
            session, document=document, generation=2, result=result,
            embeddings=[[0.1] * 1024, [0.2] * 1024],
        )

        chunks = [call.args[0] for call in session.add.call_args_list]
        self.assertEqual([chunk.content for chunk in chunks], ["Annual leave rules.", "Submit here."])
        self.assertEqual([chunk.chunk_index for chunk in chunks], [0, 1])
        self.assertEqual([chunk.section_title for chunk in chunks], ["Leave", "Requests"])
        for chunk in chunks:
            self.assertEqual(chunk.document_id, document.id)
            self.assertEqual(chunk.knowledge_base_id, document.knowledge_base_id)
            self.assertEqual(chunk.generation, 2)
            self.assertEqual(chunk.metadata_["strategy"], "paragraph")
            self.assertEqual(chunk.metadata_["chunk_size"], 200)
            self.assertEqual(chunk.metadata_["chunk_overlap"], 20)
            segment = parsed.segments[chunk.metadata_["segment_index"]]
            self.assertEqual(segment.text[chunk.metadata_["start_char"]:chunk.metadata_["end_char"]], chunk.content)
            self.assertEqual(chunk.metadata_["parser_version"], document.parser_version)
            self.assertEqual(chunk.metadata_["splitter_version"], document.splitter_version)
        self.assertEqual(document.status, "ready")
        self.assertEqual(document.active_generation, 2)
        self.assertEqual(document.chunk_count, 2)
        self.assertTrue(document.parser_version)
        self.assertTrue(document.splitter_version)


if __name__ == "__main__":
    unittest.main()
