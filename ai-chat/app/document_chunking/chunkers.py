from abc import ABC, abstractmethod
from collections.abc import Iterable

from langchain_text_splitters import RecursiveCharacterTextSplitter

from app.document_parsing.schemas import ParsedDocument
from app.document_chunking.schemas import ChunkConfig, ChunkDocument, ChunkResult


class BaseChunker(ABC):
    """共享来源保留和短块合并，具体算法只负责产生片段内字符区间。"""

    def __init__(self, config: ChunkConfig):
        self.config = config

    @abstractmethod
    def split_spans(self, text: str) -> Iterable[tuple[int, int]]:
        """返回原解析片段中的 [start, end) 字符区间。"""
        raise NotImplementedError

    def split(self, parsed: ParsedDocument) -> ChunkResult:
        chunks: list[ChunkDocument] = []
        for segment_index, segment in enumerate(parsed.segments):
            spans: list[tuple[int, int]] = []
            for start, end in self.split_spans(segment.text):
                if (
                    spans
                    and end - start < self.config.min_chunk_size
                    and end - spans[-1][0] <= self.config.chunk_size
                ):
                    spans[-1] = (spans[-1][0], end)
                else:
                    spans.append((start, end))

            for start, end in spans:
                chunks.append(ChunkDocument(
                    text=segment.text[start:end],
                    chunk_index=len(chunks),
                    page_from=segment.page_from,
                    page_to=segment.page_to,
                    section_title=segment.section_title,
                    metadata={
                        "segment_index": segment_index,
                        # 偏移基于规范化后的解析片段，不是原始文件字节位置。
                        "start_char": start,
                        "end_char": end,
                    },
                ))
        return ChunkResult(chunks=chunks, config=self.config, parser_version=parsed.parser_version)


class ParagraphChunker(BaseChunker):
    """优先保留段落边界，超长段落按换行、标点和字符递归拆分。"""

    def split_spans(self, text: str) -> Iterable[tuple[int, int]]:
        splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.config.chunk_size,
            chunk_overlap=self.config.chunk_overlap,
            separators=["\n\n", "\n", "。", "！", "？", ". ", "，", " ", ""],
            add_start_index=True,
        )
        for piece in splitter.create_documents([text]):
            start = piece.metadata["start_index"]
            yield start, start + len(piece.page_content)


class FixedSizeChunker(BaseChunker):
    """按字符窗口切分；重叠长度精确按字符计算。"""

    def split_spans(self, text: str) -> Iterable[tuple[int, int]]:
        step = self.config.chunk_size - self.config.chunk_overlap
        for start in range(0, len(text), step):
            end = min(start + self.config.chunk_size, len(text))
            window = text[start:end]
            if window.strip():
                left_trim = len(window) - len(window.lstrip())
                right_trim = len(window) - len(window.rstrip())
                yield start + left_trim, end - right_trim
            if end == len(text):
                break
