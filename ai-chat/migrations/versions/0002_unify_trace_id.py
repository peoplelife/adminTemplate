"""Unify message tracing with the shared request context."""

from alembic import op

revision = "0002_unify_trace_id"
down_revision = "0001_rag_tables"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.alter_column("messages", "request_id", new_column_name="trace_id")


def downgrade() -> None:
    op.alter_column("messages", "trace_id", new_column_name="request_id")
