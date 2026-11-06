import unittest

from app.document_parsing.schemas import ParsedDocument, ParsedSegment
from app.document_parsing.service import parse_document
from app.document_chunking.schemas import ChunkConfig, ChunkDocument, ChunkStrategy
from app.document_chunking.service import split_document


class RagSplitterTests(unittest.TestCase):
    def test_factory_selects_independent_algorithms(self):
        from app.document_chunking.chunker_factory import create_chunker
        from app.document_chunking.chunkers import FixedSizeChunker, ParagraphChunker

        for strategy, expected in (
            (ChunkStrategy.PARAGRAPH, ParagraphChunker),
            (ChunkStrategy.FIXED_SIZE, FixedSizeChunker),
        ):
            with self.subTest(strategy=strategy):
                self.assertIsInstance(create_chunker(ChunkConfig(strategy=strategy)), expected)

    def test_fixed_size_whitespace_offsets_match_source(self):
        text = "  " + "a" * 197 + "\n " + "b" * 20 + "  "
        parsed = ParsedDocument("text/plain", (ParsedSegment(text),))
        result = split_document(parsed, ChunkConfig(
            strategy=ChunkStrategy.FIXED_SIZE, chunk_size=200, chunk_overlap=20,
        ))
        self.assertEqual(result.chunks[0].text, "a" * 197)
        self.assertTrue(result.chunks[-1].text.endswith("b" * 20))
        for chunk in result.chunks:
            self.assertEqual(text[chunk.metadata["start_char"]:chunk.metadata["end_char"]], chunk.text)

    def test_splits_long_segments_and_preserves_source_metadata(self):
        parsed = ParsedDocument(
            mime_type="text/plain",
            segments=(
                ParsedSegment(
                    text="paragraph " * 60,
                    page_from=4,
                    page_to=4,
                    section_title="Leave / Requests",
                ),
            ),
        )

        result = split_document(parsed, ChunkConfig(chunk_size=200, chunk_overlap=20))
        chunks = result.chunks

        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(len(chunk.text) <= 200 for chunk in chunks))
        self.assertTrue(all(chunk.page_from == 4 and chunk.page_to == 4 for chunk in chunks))
        self.assertTrue(all(chunk.section_title == "Leave / Requests" for chunk in chunks))
        self.assertTrue(all(isinstance(chunk, ChunkDocument) for chunk in chunks))
        self.assertEqual([chunk.chunk_index for chunk in chunks], list(range(len(chunks))))
        for chunk in chunks:
            self.assertEqual(chunk.metadata["segment_index"], 0)
            self.assertEqual(
                parsed.segments[0].text[chunk.metadata["start_char"]:chunk.metadata["end_char"]],
                chunk.text,
            )

    def test_keeps_short_sections_and_never_overlaps_across_headings(self):
        parsed = parse_document("guide.md", b"# First\nYes.\n# Second\nNo.")

        result = split_document(parsed, ChunkConfig(min_chunk_size=30))

        self.assertEqual([(chunk.text, chunk.section_title) for chunk in result.chunks],
                         [("Yes.", "First"), ("No.", "Second")])
        self.assertEqual([chunk.chunk_index for chunk in result.chunks], [0, 1])

    def test_paragraph_strategy_preserves_boundaries_and_splits_oversized_paragraphs(self):
        text = "a" * 120 + "\n\n" + "b" * 120 + "\n\n" + "c" * 450
        parsed = ParsedDocument("text/plain", (ParsedSegment(text),))

        result = split_document(parsed, ChunkConfig(chunk_size=200, chunk_overlap=0))

        self.assertEqual(result.chunks[0].text, "a" * 120)
        self.assertEqual(result.chunks[1].text, "b" * 120)
        self.assertTrue(all(len(chunk.text) <= 200 for chunk in result.chunks))
        self.assertEqual("".join(chunk.text for chunk in result.chunks), text.replace("\n\n", ""))

    def test_fixed_size_overlap_is_character_based_and_preserves_short_tail(self):
        text = "a" * 200 + "b" * 15
        parsed = ParsedDocument("text/plain", (ParsedSegment(text),))

        result = split_document(parsed, ChunkConfig(
            strategy=ChunkStrategy.FIXED_SIZE, chunk_size=200, chunk_overlap=20,
        ))

        self.assertEqual([chunk.text for chunk in result.chunks], ["a" * 200, "a" * 20 + "b" * 15])
        self.assertEqual(result.chunks[1].metadata["start_char"], 180)

    def test_merges_short_tail_when_it_fits_without_losing_text(self):
        text = "a" * 205 + "\n\n" + "tail"
        parsed = ParsedDocument("text/plain", (ParsedSegment(text),))

        result = split_document(parsed, ChunkConfig(chunk_size=200, chunk_overlap=0))

        self.assertEqual([chunk.text for chunk in result.chunks], ["a" * 200, "a" * 5 + "\n\n" + "tail"])

    def test_fixed_size_keeps_tail_below_minimum_when_it_cannot_merge(self):
        parsed = ParsedDocument("text/plain", (ParsedSegment("a" * 215),))

        result = split_document(parsed, ChunkConfig(
            strategy=ChunkStrategy.FIXED_SIZE, chunk_size=200, chunk_overlap=0,
        ))

        self.assertEqual([len(chunk.text) for chunk in result.chunks], [200, 15])

    def test_rejects_invalid_chunk_configuration(self):
        parsed = ParsedDocument(
            mime_type="text/plain",
            segments=(ParsedSegment(text="content"),),
        )

        for chunk_size, overlap in ((200, 200), (200, -1), (199, 0), (1501, 0)):
            with self.subTest(chunk_size=chunk_size, overlap=overlap), self.assertRaises(ValueError):
                ChunkConfig(chunk_size=chunk_size, chunk_overlap=overlap)

        for minimum in (-1, 201):
            with self.subTest(minimum=minimum), self.assertRaises(ValueError):
                ChunkConfig(chunk_size=200, min_chunk_size=minimum)


if __name__ == "__main__":
    unittest.main()
