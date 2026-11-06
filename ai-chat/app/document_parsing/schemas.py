from dataclasses import dataclass

PARSER_VERSION = "document_parser@v1"


@dataclass(frozen=True, slots=True)
class ParsedSegment:
    text: str
    page_from: int | None = None
    page_to: int | None = None
    section_title: str | None = None


@dataclass(frozen=True, slots=True)
class ParsedDocument:
    mime_type: str
    segments: tuple[ParsedSegment, ...]
    parser_version: str = PARSER_VERSION


