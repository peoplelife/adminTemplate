from dataclasses import dataclass, field
from enum import Enum
from typing import Any

SPLITTER_VERSION = "document_chunker@v3"


class ChunkStrategy(str, Enum):
    PARAGRAPH = "paragraph"
    FIXED_SIZE = "fixed_size"


@dataclass(frozen=True, slots=True)
class ChunkConfig:
    """切分长度单位为字符；min_chunk_size 是合并目标，不是丢弃阈值。"""

    strategy: ChunkStrategy = ChunkStrategy.PARAGRAPH
    chunk_size: int = 600
    chunk_overlap: int = 100
    min_chunk_size: int = 30

    def __post_init__(self) -> None:
        if not 200 <= self.chunk_size <= 1500:
            raise ValueError("chunk_size must be between 200 and 1500")
        if not 0 <= self.chunk_overlap < self.chunk_size:
            raise ValueError("chunk_overlap must be non-negative and smaller than chunk_size")
        if not 0 <= self.min_chunk_size <= self.chunk_size:
            raise ValueError("min_chunk_size must be between 0 and chunk_size")


@dataclass(frozen=True, slots=True)
class ChunkDocument:
    """最终索引片段；来源 ID 和 generation 由入库层关联。"""

    text: str
    chunk_index: int
    page_from: int | None = None
    page_to: int | None = None
    section_title: str | None = None
    metadata: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True, slots=True)
class ChunkResult:
    chunks: list[ChunkDocument]
    config: ChunkConfig
    parser_version: str
    splitter_version: str = SPLITTER_VERSION


