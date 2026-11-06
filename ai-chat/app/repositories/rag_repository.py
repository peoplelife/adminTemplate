from datetime import UTC, datetime
import hashlib
from uuid import UUID

from sqlalchemy import and_, delete, desc, func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.rag import Chunk, Document, KnowledgeBase, MessageSource, RagConversation, RagMessage
from app.document_chunking.schemas import ChunkResult


class RagRepository:
    async def create_conversation(self, session, *, owner_id, title, knowledge_base_id, model):
        conversation = RagConversation(
            owner_id=owner_id, title=title, knowledge_base_id=knowledge_base_id, model=model
        )
        session.add(conversation)
        await session.flush()
        return conversation

    async def get_conversation(self, session, *, owner_id, conversation_id, for_update=False):
        statement = select(RagConversation).where(
            RagConversation.id == conversation_id,
            RagConversation.owner_id == owner_id,
            RagConversation.status != "deleted",
        )
        if for_update:
            statement = statement.with_for_update()
        return (await session.execute(statement)).scalar_one_or_none()

    async def list_conversations(self, session, *, owner_id, page, page_size, mode="all", knowledge_base_id=None):
        statement = select(RagConversation).where(
            RagConversation.owner_id == owner_id, RagConversation.status != "deleted"
        )
        if mode == "chat":
            statement = statement.where(RagConversation.knowledge_base_id.is_(None))
        elif mode == "knowledge":
            statement = statement.where(RagConversation.knowledge_base_id.is_not(None))
        if knowledge_base_id is not None:
            statement = statement.where(RagConversation.knowledge_base_id == knowledge_base_id)
        total = (await session.execute(select(func.count()).select_from(statement.subquery()))).scalar_one()
        rows = (await session.execute(statement.order_by(desc(RagConversation.updated_at), desc(RagConversation.id))
            .offset((page - 1) * page_size).limit(page_size))).scalars().all()
        return list(rows), total

    async def update_conversation(self, session, *, conversation, title=None, status=None):
        if title is not None:
            conversation.title = title
        if status is not None:
            conversation.status = status
        await session.flush()
        return conversation

    async def soft_delete_conversation(self, session, *, conversation):
        conversation.status = "deleted"
        conversation.deleted_at = datetime.now(UTC)
        await session.execute(update(RagMessage).where(
            RagMessage.conversation_id == conversation.id,
            RagMessage.role == "assistant",
            RagMessage.status == "generating",
        ).values(status="cancelled"))
        await session.flush()

    async def list_conversation_messages(self, session, *, owner_id, conversation_id, before_seq=None, limit=100):
        statement = (select(RagMessage).join(RagConversation, RagConversation.id == RagMessage.conversation_id)
            .where(RagConversation.owner_id == owner_id, RagConversation.id == conversation_id,
                   RagConversation.status != "deleted"))
        if before_seq is not None:
            statement = statement.where(RagMessage.seq < before_seq)
        rows = (await session.execute(statement.order_by(RagMessage.seq.desc()).limit(limit))).scalars().all()
        visible = list(reversed(rows))
        sources = await self.get_message_sources(session, message_ids=[message.id for message in visible])
        return [(message, sources.get(message.id, [])) for message in visible], (visible[0].seq if len(rows) == limit else None)

    async def get_message_sources(self, session, *, message_ids):
        if not message_ids:
            return {}
        rows = (await session.execute(select(MessageSource).where(MessageSource.message_id.in_(message_ids))
            .order_by(MessageSource.citation_index))).scalars().all()
        grouped = {}
        for source in rows:
            grouped.setdefault(source.message_id, []).append(source)
        return grouped

    async def find_request_messages(self, session, *, owner_id, conversation_id, client_request_id):
        rows = (await session.execute(select(RagMessage).join(
            RagConversation, RagConversation.id == RagMessage.conversation_id
        ).where(
            RagConversation.owner_id == owner_id,
            RagConversation.status != "deleted",
            RagMessage.conversation_id == conversation_id,
            RagMessage.client_request_id == client_request_id,
        ).order_by(RagMessage.seq))).scalars().all()
        return rows

    async def has_active_generation(self, session, *, owner_id, conversation_id):
        return (await session.execute(select(RagMessage.id).join(
            RagConversation, RagConversation.id == RagMessage.conversation_id
        ).where(
            RagConversation.owner_id == owner_id,
            RagConversation.status != "deleted",
            RagMessage.conversation_id == conversation_id,
            RagMessage.role == "assistant",
            RagMessage.status == "generating",
        ).limit(1))).scalar_one_or_none() is not None

    async def get_recent_messages(self, session, *, owner_id, conversation_id, before_seq, limit=12):
        rows = (await session.execute(select(RagMessage).join(
            RagConversation, RagConversation.id == RagMessage.conversation_id
        ).where(
            RagConversation.owner_id == owner_id,
            RagConversation.status != "deleted",
            RagMessage.conversation_id == conversation_id,
            RagMessage.seq < before_seq,
            RagMessage.status == "completed",
        ).order_by(RagMessage.seq.desc()).limit(limit))).scalars().all()
        return list(reversed(rows))

    async def create_message_pair(self, session, *, conversation, client_request_id, request_hash, content, trace_id):
        now = datetime.now(UTC)
        user = RagMessage(
            conversation_id=conversation.id, seq=conversation.next_message_seq,
            client_request_id=client_request_id, request_hash=request_hash, role="user",
            content=content, status="completed", model=conversation.model, trace_id=trace_id,
        )
        assistant = RagMessage(
            conversation_id=conversation.id, seq=conversation.next_message_seq + 1,
            client_request_id=client_request_id, request_hash=request_hash, role="assistant",
            content="", status="generating", model=conversation.model, trace_id=trace_id,
        )
        conversation.next_message_seq += 2
        conversation.updated_at = now
        session.add_all([user, assistant])
        await session.flush()
        return user, assistant

    async def create_message_sources(self, session, *, message_id, items):
        for index, item in enumerate(items, 1):
            session.add(MessageSource(
                message_id=message_id, citation_index=index, chunk_id=item.chunk_id,
                document_id_snapshot=item.document_id, generation_snapshot=item.generation,
                document_name=item.file_name, page_from=item.page_from, page_to=item.page_to,
                section_title=item.section_title, content_excerpt=item.content[:1200],
                vector_similarity=item.similarity,
            ))
        await session.flush()

    async def complete_assistant_message(self, session, *, assistant_id, content, finish_reason, usage):
        now = datetime.now(UTC)
        result = await session.execute(update(RagMessage).where(
            RagMessage.id == assistant_id, RagMessage.role == "assistant",
            RagMessage.status == "generating",
        ).values(content=content, status="completed", finish_reason=finish_reason,
                 prompt_tokens=usage.get("prompt_tokens", 0),
                 completion_tokens=usage.get("completion_tokens", 0),
                 total_tokens=usage.get("total_tokens", 0), completed_at=now))
        return result.rowcount == 1

    async def fail_assistant_message(self, session, *, assistant_id, content, code, message):
        await session.execute(update(RagMessage).where(
            RagMessage.id == assistant_id,
            RagMessage.role == "assistant",
            RagMessage.status == "generating",
        ).values(content=content, status="failed", error_code=code,
                 error_message=message[:500]))

    async def cancel_assistant_message(self, session, *, assistant_id, content):
        await session.execute(update(RagMessage).where(
            RagMessage.id == assistant_id,
            RagMessage.role == "assistant",
            RagMessage.status == "generating",
        ).values(content=content, status="cancelled"))

    async def get_retrieval_profile(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
    ) -> tuple[KnowledgeBase, bool] | None:
        has_ready_document = (
            select(Document.id)
            .where(
                Document.knowledge_base_id == KnowledgeBase.id,
                Document.status == "ready",
                Document.active_generation > 0,
                Document.deleted_at.is_(None),
            )
            .exists()
        )
        statement = select(KnowledgeBase, has_ready_document.label("has_ready_document")).where(
            KnowledgeBase.id == knowledge_base_id,
            KnowledgeBase.owner_id == owner_id,
            KnowledgeBase.status != "deleted",
        )
        row = (await session.execute(statement)).one_or_none()
        return None if row is None else (row[0], bool(row[1]))

    async def search_active_chunks(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        query_embedding: list[float],
        limit: int,
    ) -> list[tuple[Chunk, str, float]]:
        distance = Chunk.embedding.cosine_distance(query_embedding)
        statement = (
            select(Chunk, Document.file_name, distance.label("cosine_distance"))
            .select_from(Chunk)
            .join(
                Document,
                and_(
                    Chunk.document_id == Document.id,
                    Chunk.knowledge_base_id == Document.knowledge_base_id,
                ),
            )
            .join(KnowledgeBase, KnowledgeBase.id == Document.knowledge_base_id)
            .where(
                KnowledgeBase.id == knowledge_base_id,
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status == "active",
                KnowledgeBase.deleted_at.is_(None),
                Document.status == "ready",
                Document.deleted_at.is_(None),
                Document.active_generation > 0,
                Chunk.knowledge_base_id == knowledge_base_id,
                Chunk.generation == Document.active_generation,
            )
            .order_by(distance, Chunk.id)
            .limit(limit)
        )
        rows = (await session.execute(statement)).all()
        return [(row[0], row[1], float(row[2])) for row in rows]

    async def create_knowledge_base(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        name: str,
        description: str | None,
        embedding_model: str,
    ) -> KnowledgeBase:
        knowledge_base = KnowledgeBase(
            owner_id=owner_id,
            name=name,
            description=description,
            embedding_model=embedding_model,
        )
        session.add(knowledge_base)
        await session.flush()
        return knowledge_base

    async def get_knowledge_base(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
    ) -> tuple[KnowledgeBase, int, int] | None:
        counts = self._document_counts()
        statement = (
            select(
                KnowledgeBase,
                func.coalesce(counts.c.document_count, 0),
                func.coalesce(counts.c.ready_document_count, 0),
            )
            .outerjoin(counts, counts.c.knowledge_base_id == KnowledgeBase.id)
            .where(
                KnowledgeBase.id == knowledge_base_id,
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status != "deleted",
            )
        )
        row = (await session.execute(statement)).one_or_none()
        return None if row is None else (row[0], int(row[1]), int(row[2]))

    async def list_knowledge_bases(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        page: int,
        page_size: int,
    ) -> tuple[list[tuple[KnowledgeBase, int, int]], int]:
        counts = self._document_counts()
        statement = (
            select(
                KnowledgeBase,
                func.coalesce(counts.c.document_count, 0),
                func.coalesce(counts.c.ready_document_count, 0),
            )
            .outerjoin(counts, counts.c.knowledge_base_id == KnowledgeBase.id)
            .where(
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status != "deleted",
            )
        )
        total = (await session.execute(select(func.count()).select_from(statement.subquery()))).scalar_one()
        rows = (
            await session.execute(
                statement.order_by(desc(KnowledgeBase.updated_at), desc(KnowledgeBase.id)).offset((page - 1) * page_size).limit(page_size)
            )
        ).all()
        return [(row[0], int(row[1]), int(row[2])) for row in rows], total

    async def get_knowledge_base_for_update(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
    ) -> KnowledgeBase | None:
        statement = (
            select(KnowledgeBase)
            .where(
                KnowledgeBase.id == knowledge_base_id,
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status != "deleted",
            )
            .with_for_update()
        )
        return (await session.execute(statement)).scalar_one_or_none()

    async def find_document_by_hash(
        self,
        session: AsyncSession,
        *,
        knowledge_base_id: UUID,
        file_hash: str,
    ) -> Document | None:
        return (
            await session.execute(
                select(Document).where(
                    Document.knowledge_base_id == knowledge_base_id,
                    Document.file_hash == file_hash,
                    Document.status != "deleted",
                )
            )
        ).scalar_one_or_none()

    async def create_document(
        self,
        session: AsyncSession,
        *,
        knowledge_base_id: UUID,
        created_by: str,
        file_name: str,
        storage_key: str,
        file_hash: str,
        mime_type: str,
        file_size: int,
        generation: int,
    ) -> Document:
        document = Document(
            knowledge_base_id=knowledge_base_id,
            created_by=created_by,
            file_name=file_name,
            storage_key=storage_key,
            file_hash=file_hash,
            mime_type=mime_type,
            file_size=file_size,
            latest_generation=generation,
        )
        session.add(document)
        await session.flush()
        return document

    async def list_documents(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        page: int,
        page_size: int,
    ) -> tuple[list[Document], int]:
        statement = (
            select(Document)
            .join(KnowledgeBase, KnowledgeBase.id == Document.knowledge_base_id)
            .where(
                Document.knowledge_base_id == knowledge_base_id,
                Document.status != "deleted",
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status != "deleted",
            )
        )
        total = (await session.execute(select(func.count()).select_from(statement.subquery()))).scalar_one()
        rows = (
            await session.execute(
                statement.order_by(desc(Document.created_at), desc(Document.id)).offset((page - 1) * page_size).limit(page_size)
            )
        ).all()
        return [row[0] for row in rows], total

    async def get_document(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        document_id: UUID,
    ) -> Document | None:
        statement = (
            select(Document)
            .join(KnowledgeBase, KnowledgeBase.id == Document.knowledge_base_id)
            .where(
                Document.id == document_id,
                Document.knowledge_base_id == knowledge_base_id,
                Document.status != "deleted",
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status != "deleted",
            )
        )
        return (await session.execute(statement)).scalar_one_or_none()

    async def get_document_for_update(
        self,
        session: AsyncSession,
        *,
        owner_id: str,
        knowledge_base_id: UUID,
        document_id: UUID,
    ) -> tuple[Document, KnowledgeBase] | None:
        statement = (
            select(Document, KnowledgeBase)
            .join(KnowledgeBase, KnowledgeBase.id == Document.knowledge_base_id)
            .where(
                Document.id == document_id,
                Document.knowledge_base_id == knowledge_base_id,
                Document.status != "deleted",
                KnowledgeBase.owner_id == owner_id,
                KnowledgeBase.status != "deleted",
            )
            .with_for_update(of=Document)
        )
        row = (await session.execute(statement)).one_or_none()
        return None if row is None else (row[0], row[1])

    async def replace_document_chunks(
        self,
        session: AsyncSession,
        *,
        document: Document,
        generation: int,
        result: ChunkResult,
        embeddings,
    ) -> None:
        now = datetime.now(UTC)
        await session.execute(delete(Chunk).where(Chunk.document_id == document.id))
        config = result.config
        for chunk, embedding in zip(result.chunks, embeddings, strict=True):
            session.add(
                Chunk(
                    knowledge_base_id=document.knowledge_base_id,
                    document_id=document.id,
                    generation=generation,
                    chunk_index=chunk.chunk_index,
                    content=chunk.text,
                    content_hash=hashlib.sha256(chunk.text.encode()).hexdigest(),
                    token_count=max(1, len(chunk.text)),
                    page_from=chunk.page_from,
                    page_to=chunk.page_to,
                    section_title=chunk.section_title,
                    metadata_={
                        **chunk.metadata,
                        "strategy": config.strategy.value,
                        "chunk_size": config.chunk_size,
                        "chunk_overlap": config.chunk_overlap,
                        "min_chunk_size": config.min_chunk_size,
                        "length_unit": "characters",
                        "token_count_method": "character_estimate",
                        "parser_version": result.parser_version,
                        "splitter_version": result.splitter_version,
                    },
                    embedding=embedding,
                )
            )
        document.status = "ready"
        document.active_generation = generation
        document.latest_generation = generation
        document.chunk_count = len(result.chunks)
        document.page_count = max((chunk.page_to or 0 for chunk in result.chunks), default=0) or None
        document.parser_version = result.parser_version
        document.splitter_version = result.splitter_version
        document.error_code = None
        document.error_message = None
        document.updated_at = now
        await session.flush()

    @staticmethod
    def _document_counts():
        return (
            select(
                Document.knowledge_base_id,
                func.count(Document.id).label("document_count"),
                func.count(Document.id)
                .filter(Document.status == "ready")
                .label("ready_document_count"),
            )
            .where(Document.status != "deleted")
            .group_by(Document.knowledge_base_id)
            .subquery()
        )


rag_repository = RagRepository()
