"""Create online retrieval evaluation tables in the public schema."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql as pg

revision = "0003_create_evaluation_tables"
down_revision = "0002_unify_trace_id"
branch_labels = None
depends_on = None


def upgrade():
    op.create_table("evaluation_datasets",
        sa.Column("id", pg.UUID(as_uuid=True), primary_key=True),
        sa.Column("owner_id", sa.String(64), nullable=False),
        sa.Column("knowledge_base_id", pg.UUID(as_uuid=True), sa.ForeignKey("knowledge_bases.id"), nullable=False),
        sa.Column("name", sa.String(100), nullable=False),
        sa.Column("description", sa.Text(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_evaluation_datasets_owner_created", "evaluation_datasets", ["owner_id", "created_at"])
    op.create_table("evaluation_cases",
        sa.Column("id", pg.UUID(as_uuid=True), primary_key=True),
        sa.Column("dataset_id", pg.UUID(as_uuid=True), sa.ForeignKey("evaluation_datasets.id", ondelete="CASCADE"), nullable=False),
        sa.Column("data", pg.JSONB(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
    )
    op.create_index("ix_evaluation_cases_dataset_created", "evaluation_cases", ["dataset_id", "created_at"])
    op.create_table("evaluation_runs",
        sa.Column("id", pg.UUID(as_uuid=True), primary_key=True),
        sa.Column("dataset_id", pg.UUID(as_uuid=True), sa.ForeignKey("evaluation_datasets.id", ondelete="CASCADE"), nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("top_k", sa.Integer(), nullable=False),
        sa.Column("total_cases", sa.Integer(), nullable=False),
        sa.Column("metrics", pg.JSONB(), nullable=False),
        sa.Column("config", pg.JSONB(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True)),
    )
    op.create_index("ix_evaluation_runs_dataset_created", "evaluation_runs", ["dataset_id", "created_at"])
    op.create_table("evaluation_results",
        sa.Column("id", pg.UUID(as_uuid=True), primary_key=True),
        sa.Column("run_id", pg.UUID(as_uuid=True), sa.ForeignKey("evaluation_runs.id", ondelete="CASCADE"), nullable=False),
        sa.Column("case_id", pg.UUID(as_uuid=True), sa.ForeignKey("evaluation_cases.id", ondelete="SET NULL")),
        sa.Column("case_snapshot", pg.JSONB(), nullable=False),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("retrieved_items", pg.JSONB(), nullable=False),
        sa.Column("hit_at_k", sa.Float()), sa.Column("recall_at_k", sa.Float()), sa.Column("mrr", sa.Float()),
        sa.Column("elapsed_ms", sa.Float(), nullable=False), sa.Column("error_code", sa.Integer()),
    )
    op.create_index("ix_evaluation_results_run", "evaluation_results", ["run_id"])


def downgrade():
    for table in ("evaluation_results", "evaluation_runs", "evaluation_cases", "evaluation_datasets"):
        op.drop_table(table)
