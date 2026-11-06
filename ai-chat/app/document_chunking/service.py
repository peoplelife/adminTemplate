from app.document_parsing.schemas import ParsedDocument
from app.document_chunking.chunker_factory import create_chunker
from app.document_chunking.schemas import ChunkConfig, ChunkResult


def split_document(parsed: ParsedDocument, config: ChunkConfig) -> ChunkResult:
    """统一切分入口，由工厂选择算法，保留解析来源信息。"""
    return create_chunker(config).split(parsed)
