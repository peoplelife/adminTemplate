import unittest
from io import BytesIO

from docx import Document as WordDocument
from pypdf import PdfWriter

from app.document_parsing.service import DocumentInputError, parse_document


class RagDocumentParserTests(unittest.TestCase):
    def test_parses_utf8_text(self):
        parsed = parse_document("guide.txt", "员工手册".encode("utf-8"))

        self.assertEqual(parsed.mime_type, "text/plain")
        self.assertEqual([segment.text for segment in parsed.segments], ["员工手册"])

    def test_parses_markdown_sections(self):
        parsed = parse_document(
            "guide.md",
            "# Leave\nAnnual leave rules.\n\n## Requests\nSubmit a request.".encode("utf-8"),
        )

        self.assertEqual(
            [(segment.section_title, segment.text) for segment in parsed.segments],
            [
                ("Leave", "Annual leave rules."),
                ("Leave / Requests", "Submit a request."),
            ],
        )

    def test_parses_docx_headings_paragraphs_and_tables(self):
        document = WordDocument()
        document.add_heading("Leave", level=1)
        document.add_paragraph("Annual leave rules.")
        table = document.add_table(rows=1, cols=2)
        table.cell(0, 0).text = "Days"
        table.cell(0, 1).text = "10"
        content = BytesIO()
        document.save(content)

        parsed = parse_document("guide.docx", content.getvalue())

        self.assertEqual(parsed.mime_type, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        self.assertEqual(len(parsed.segments), 1)
        self.assertEqual(parsed.segments[0].section_title, "Leave")
        self.assertIn("Annual leave rules.", parsed.segments[0].text)
        self.assertIn("Days | 10", parsed.segments[0].text)

    def test_rejects_textless_pdf_as_unsupported_ocr(self):
        writer = PdfWriter()
        writer.add_blank_page(width=72, height=72)
        content = BytesIO()
        writer.write(content)

        with self.assertRaisesRegex(DocumentInputError, "OCR"):
            parse_document("scan.pdf", content.getvalue())

    def test_rejects_unsupported_empty_and_invalid_utf8_files(self):
        cases = (
            ("legacy.doc", b"not supported"),
            ("empty.txt", b""),
            ("broken.txt", b"\xff"),
        )

        for file_name, content in cases:
            with self.subTest(file_name=file_name), self.assertRaises(DocumentInputError):
                parse_document(file_name, content)

    def test_rejects_upload_and_extracted_text_over_limits(self):
        with self.assertRaisesRegex(DocumentInputError, "20 MiB"):
            parse_document("large.txt", b"x" * (20 * 1024 * 1024 + 1))

        with self.assertRaisesRegex(DocumentInputError, "extracted text limit"):
            parse_document("large.txt", b"x" * 1_000_001)

    def test_rejects_pdf_over_page_limit(self):
        writer = PdfWriter()
        for _ in range(501):
            writer.add_blank_page(width=72, height=72)
        content = BytesIO()
        writer.write(content)

        with self.assertRaisesRegex(DocumentInputError, "500 pages"):
            parse_document("large.pdf", content.getvalue())


if __name__ == "__main__":
    unittest.main()
