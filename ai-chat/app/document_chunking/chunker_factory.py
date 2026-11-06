from app.document_chunking.chunkers import BaseChunker, FixedSizeChunker, ParagraphChunker
from app.document_chunking.schemas import ChunkConfig, ChunkStrategy


def create_chunker(config: ChunkConfig) -> BaseChunker:
    """集中维护策略与算法的对应关系。"""
    chunkers = {
        ChunkStrategy.PARAGRAPH: ParagraphChunker,
        ChunkStrategy.FIXED_SIZE: FixedSizeChunker,
    }
    return chunkers[config.strategy](config)
