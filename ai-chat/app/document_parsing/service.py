import re
from io import BytesIO
from pathlib import Path
from zipfile import BadZipFile, LargeZipFile, ZipFile

from docx import Document as WordDocument
from docx.opc.exceptions import PackageNotFoundError
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph
from pypdf import PdfReader
from pypdf.errors import PdfReadError

from app.document_parsing.schemas import ParsedDocument, ParsedSegment

MAX_UPLOAD_BYTES = 20 * 1024 * 1024
MAX_PDF_PAGES = 500
MAX_DOCX_EXPANDED_BYTES = 100 * 1024 * 1024
MAX_EXTRACTED_CHARACTERS = 1_000_000

MIME_TYPES = {
    ".pdf": "application/pdf",
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".txt": "text/plain",
    ".md": "text/markdown",
}


class DocumentInputError(ValueError):
    pass


def parse_document(file_name: str, content: bytes) -> ParsedDocument:
    extension = Path(file_name).suffix.lower()
    mime_type = MIME_TYPES.get(extension)
    if mime_type is None:
        raise DocumentInputError("File format is not supported")
    if not content:
        raise DocumentInputError("File is empty")
    if len(content) > MAX_UPLOAD_BYTES:
        raise DocumentInputError("File exceeds the 20 MiB upload limit")

    if extension == ".pdf":
        if b"%PDF-" not in content[:1024]:
            raise DocumentInputError("File content does not match PDF format")
        segments = _parse_pdf(content)
    elif extension == ".docx":
        segments = _parse_docx(content)
    else:
        segments = _parse_text(extension, content)

    if not segments:
        raise DocumentInputError("File contains no extractable text")
    if sum(len(segment.text) for segment in segments) > MAX_EXTRACTED_CHARACTERS:
        raise DocumentInputError("File exceeds the extracted text limit")

    return ParsedDocument(mime_type=mime_type, segments=tuple(segments))


def _parse_text(extension: str, content: bytes) -> list[ParsedSegment]:
    try:
        text = content.decode("utf-8-sig")
    except UnicodeDecodeError:
        raise DocumentInputError("Text files must use UTF-8 encoding") from None

    if any(ord(character) < 32 and character not in "\n\r\t" for character in text):
        raise DocumentInputError("Text file contains unsupported control characters")

    text = _normalize_text(text)
    if not text:
        return []
    if extension == ".md":
        return _markdown_sections(text)
    return [ParsedSegment(text=text)]


def _markdown_sections(text: str) -> list[ParsedSegment]:
    segments: list[ParsedSegment] = []
    heading_stack: list[str] = []
    body_lines: list[str] = []

    def flush() -> None:
        body = _normalize_text("\n".join(body_lines))
        if body:
            segments.append(
                ParsedSegment(
                    text=body,
                    section_title=" / ".join(heading_stack) or None,
                )
            )
        body_lines.clear()

    for line in text.splitlines():
        heading = re.match(r"^\s{0,3}(#{1,6})\s+(.+?)\s*#*\s*$", line)
        if heading is None:
            body_lines.append(line)
            continue

        flush()
        level = len(heading.group(1))
        heading_stack = heading_stack[: level - 1]
        heading_stack.append(heading.group(2).strip())

    flush()
    return segments


def _parse_pdf(content: bytes) -> list[ParsedSegment]:
    try:
        reader = PdfReader(BytesIO(content), strict=False)
        if reader.is_encrypted:
            raise DocumentInputError("Password-protected PDFs are not supported")
        if len(reader.pages) > MAX_PDF_PAGES:
            raise DocumentInputError("PDF exceeds the 500 pages limit")

        segments = []
        extracted_characters = 0
        for page_number, page in enumerate(reader.pages, start=1):
            text = _normalize_text(page.extract_text() or "")
            if not text:
                continue
            extracted_characters += len(text)
            if extracted_characters > MAX_EXTRACTED_CHARACTERS:
                raise DocumentInputError("File exceeds the extracted text limit")
            segments.append(
                ParsedSegment(text=text, page_from=page_number, page_to=page_number)
            )
    except DocumentInputError:
        raise
    except (PdfReadError, ValueError, KeyError, OSError):
        raise DocumentInputError("PDF is damaged or cannot be read") from None

    if not segments:
        raise DocumentInputError("PDF contains no extractable text; OCR is not supported")
    return segments


def _parse_docx(content: bytes) -> list[ParsedSegment]:
    try:
        with ZipFile(BytesIO(content)) as archive:
            names = set(archive.namelist())
            if "[Content_Types].xml" not in names or "word/document.xml" not in names:
                raise DocumentInputError("File content does not match DOCX format")
            if sum(item.file_size for item in archive.infolist()) > MAX_DOCX_EXPANDED_BYTES:
                raise DocumentInputError("DOCX exceeds the 100 MiB expanded size limit")
        document = WordDocument(BytesIO(content))
    except DocumentInputError:
        raise
    except (BadZipFile, LargeZipFile, PackageNotFoundError, ValueError, OSError):
        raise DocumentInputError("DOCX is damaged or cannot be read") from None

    segments: list[ParsedSegment] = []
    heading_stack: list[str] = []
    body_lines: list[str] = []
    extracted_characters = 0

    def flush() -> None:
        nonlocal extracted_characters
        body = _normalize_text("\n".join(body_lines))
        if body:
            extracted_characters += len(body)
            if extracted_characters > MAX_EXTRACTED_CHARACTERS:
                raise DocumentInputError("File exceeds the extracted text limit")
            segments.append(
                ParsedSegment(
                    text=body,
                    section_title=" / ".join(heading_stack) or None,
                )
            )
        body_lines.clear()

    for block in document.element.body.iterchildren():
        if isinstance(block, CT_P):
            paragraph = Paragraph(block, document)
            paragraph_text = paragraph.text.strip()
            heading = _word_heading(paragraph.style.name, paragraph_text)
            if heading is not None:
                flush()
                level, title = heading
                heading_stack = heading_stack[: level - 1]
                heading_stack.append(title)
            elif paragraph_text:
                body_lines.append(paragraph_text)
        elif isinstance(block, CT_Tbl):
            table = Table(block, document)
            for row in table.rows:
                row_text = " | ".join(cell.text.strip() for cell in row.cells).strip()
                if row_text:
                    body_lines.append(row_text)

    flush()
    return segments


def _word_heading(style_name: str, text: str) -> tuple[int, str] | None:
    match = re.fullmatch(r"(?:heading|标题)\s*([1-6])", style_name.strip(), re.IGNORECASE)
    if match is None or not text:
        return None
    return int(match.group(1)), text


def _normalize_text(text: str) -> str:
    return text.replace("\x00", "").replace("\r\n", "\n").replace("\r", "\n").strip()
