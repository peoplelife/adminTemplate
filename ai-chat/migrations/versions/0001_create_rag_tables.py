from alembic import op

revision = "0001_rag_tables"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("CREATE EXTENSION IF NOT EXISTS vector")
    op.execute(
        """
        CREATE TABLE knowledge_bases (
            id UUID PRIMARY KEY,
            owner_id VARCHAR(64) NOT NULL,
            name VARCHAR(100) NOT NULL,
            description VARCHAR(1000),
            status VARCHAR(20) NOT NULL DEFAULT 'active',
            embedding_model VARCHAR(150) NOT NULL,
            embedding_dimension SMALLINT NOT NULL DEFAULT 1024,
            chunk_size INTEGER NOT NULL DEFAULT 600,
            chunk_overlap INTEGER NOT NULL DEFAULT 100,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            deleted_at TIMESTAMPTZ,
            CONSTRAINT ck_knowledge_bases_status
                CHECK (status IN ('active', 'disabled', 'deleted')),
            CONSTRAINT ck_knowledge_bases_embedding_dimension
                CHECK (embedding_dimension = 1024),
            CONSTRAINT ck_knowledge_bases_chunk_size
                CHECK (chunk_size BETWEEN 200 AND 1500),
            CONSTRAINT ck_knowledge_bases_chunk_overlap
                CHECK (chunk_overlap >= 0 AND chunk_overlap < chunk_size)
        )
        """
    )
    op.execute(
        """
        CREATE TABLE documents (
            id UUID PRIMARY KEY,
            knowledge_base_id UUID NOT NULL REFERENCES knowledge_bases(id),
            created_by VARCHAR(64) NOT NULL,
            file_name VARCHAR(255) NOT NULL,
            storage_key VARCHAR(500) NOT NULL,
            file_hash CHAR(64) NOT NULL,
            mime_type VARCHAR(100) NOT NULL,
            file_size BIGINT NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'pending',
            active_generation INTEGER NOT NULL DEFAULT 0,
            latest_generation INTEGER NOT NULL DEFAULT 0,
            chunk_count INTEGER NOT NULL DEFAULT 0,
            page_count INTEGER,
            parser_version VARCHAR(50),
            splitter_version VARCHAR(50),
            error_code INTEGER,
            error_message VARCHAR(500),
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            deleted_at TIMESTAMPTZ,
            purged_at TIMESTAMPTZ,
            CONSTRAINT ck_documents_status
                CHECK (status IN ('pending', 'processing', 'ready', 'failed', 'deleted')),
            CONSTRAINT ck_documents_file_size CHECK (file_size > 0),
            CONSTRAINT ck_documents_generation
                CHECK (active_generation >= 0 AND latest_generation >= active_generation),
            CONSTRAINT uq_documents_id_knowledge_base UNIQUE (id, knowledge_base_id)
        )
        """
    )
    op.execute(
        """
        CREATE TABLE chunks (
            id UUID PRIMARY KEY,
            knowledge_base_id UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
            document_id UUID NOT NULL,
            generation INTEGER NOT NULL,
            chunk_index INTEGER NOT NULL,
            content TEXT NOT NULL,
            content_hash CHAR(64) NOT NULL,
            token_count INTEGER NOT NULL,
            page_from INTEGER,
            page_to INTEGER,
            section_title VARCHAR(500),
            metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
            embedding VECTOR(1024) NOT NULL,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT ck_chunks_index CHECK (chunk_index >= 0),
            CONSTRAINT ck_chunks_generation CHECK (generation > 0),
            CONSTRAINT ck_chunks_token_count CHECK (token_count > 0),
            CONSTRAINT uq_chunks_document_generation_index
                UNIQUE (document_id, generation, chunk_index),
            CONSTRAINT fk_chunks_document_knowledge_base
                FOREIGN KEY (document_id, knowledge_base_id)
                REFERENCES documents(id, knowledge_base_id) ON DELETE CASCADE
        )
        """
    )
    op.execute(
        """
        CREATE TABLE conversations (
            id UUID PRIMARY KEY,
            owner_id VARCHAR(64) NOT NULL,
            knowledge_base_id UUID REFERENCES knowledge_bases(id),
            title VARCHAR(200) NOT NULL DEFAULT '新会话',
            model VARCHAR(100) NOT NULL,
            status VARCHAR(20) NOT NULL DEFAULT 'active',
            next_message_seq BIGINT NOT NULL DEFAULT 1,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            deleted_at TIMESTAMPTZ,
            CONSTRAINT ck_conversations_status
                CHECK (status IN ('active', 'archived', 'deleted')),
            CONSTRAINT ck_conversations_next_message_seq CHECK (next_message_seq > 0)
        )
        """
    )
    op.execute(
        """
        CREATE TABLE messages (
            id UUID PRIMARY KEY,
            conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
            seq BIGINT NOT NULL,
            client_request_id UUID NOT NULL,
            request_hash CHAR(64) NOT NULL,
            role VARCHAR(20) NOT NULL,
            content TEXT NOT NULL DEFAULT '',
            status VARCHAR(20) NOT NULL,
            model VARCHAR(100),
            retrieval_query TEXT,
            prompt_version VARCHAR(50),
            prompt_tokens INTEGER,
            completion_tokens INTEGER,
            total_tokens INTEGER,
            finish_reason VARCHAR(50),
            error_code INTEGER,
            error_message VARCHAR(500),
            request_id VARCHAR(128) NOT NULL,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            completed_at TIMESTAMPTZ,
            CONSTRAINT ck_messages_seq CHECK (seq > 0),
            CONSTRAINT ck_messages_role CHECK (role IN ('user', 'assistant')),
            CONSTRAINT ck_messages_status
                CHECK (status IN ('completed', 'generating', 'failed', 'cancelled')),
            CONSTRAINT uq_messages_conversation_seq UNIQUE (conversation_id, seq),
            CONSTRAINT uq_messages_conversation_request_role
                UNIQUE (conversation_id, client_request_id, role)
        )
        """
    )
    op.execute(
        """
        CREATE TABLE message_sources (
            id UUID PRIMARY KEY,
            message_id UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
            citation_index SMALLINT NOT NULL,
            chunk_id UUID REFERENCES chunks(id) ON DELETE SET NULL,
            document_id_snapshot UUID NOT NULL,
            generation_snapshot INTEGER NOT NULL,
            document_name VARCHAR(255) NOT NULL,
            page_from INTEGER,
            page_to INTEGER,
            section_title VARCHAR(500),
            content_excerpt TEXT NOT NULL,
            vector_similarity DOUBLE PRECISION,
            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
            CONSTRAINT ck_message_sources_citation_index CHECK (citation_index > 0),
            CONSTRAINT ck_message_sources_generation CHECK (generation_snapshot > 0),
            CONSTRAINT uq_message_sources_citation UNIQUE (message_id, citation_index)
        )
        """
    )
    op.execute(
        "CREATE INDEX ix_knowledge_bases_owner_status_updated ON knowledge_bases (owner_id, status, updated_at DESC, id DESC)"
    )
    op.execute(
        "CREATE INDEX ix_documents_kb_status_created ON documents (knowledge_base_id, status, created_at DESC, id DESC)"
    )
    op.execute(
        "CREATE UNIQUE INDEX uq_documents_kb_file_hash_live ON documents (knowledge_base_id, file_hash) WHERE status <> 'deleted'"
    )
    op.execute(
        "CREATE INDEX ix_chunks_kb_document_generation ON chunks (knowledge_base_id, document_id, generation)"
    )
    op.execute(
        "CREATE INDEX ix_conversations_owner_status_updated ON conversations (owner_id, status, updated_at DESC, id DESC)"
    )
    op.execute(
        "CREATE INDEX ix_conversations_owner_kb_updated ON conversations (owner_id, knowledge_base_id, updated_at DESC, id DESC)"
    )
    op.execute(
        "CREATE UNIQUE INDEX uq_messages_active_generation ON messages (conversation_id) WHERE role = 'assistant' AND status = 'generating'"
    )
    op.execute("CREATE INDEX ix_message_sources_message ON message_sources (message_id)")


def downgrade() -> None:
    op.execute("DROP TABLE message_sources")
    op.execute("DROP TABLE messages")
    op.execute("DROP TABLE conversations")
    op.execute("DROP TABLE chunks")
    op.execute("DROP TABLE documents")
    op.execute("DROP TABLE knowledge_bases")
