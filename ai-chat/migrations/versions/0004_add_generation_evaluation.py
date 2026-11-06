"""Store generated answers and generation evaluation metrics."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql as pg

revision = "0004_add_generation_evaluation"
down_revision = "0003_create_evaluation_tables"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("evaluation_results", sa.Column("generated_answer", sa.Text()))
    op.add_column("evaluation_results", sa.Column("generation_model", sa.String(150)))
    op.add_column("evaluation_results", sa.Column("prompt_version", sa.String(100)))
    op.add_column("evaluation_results", sa.Column("generation_status", sa.String(20), nullable=False, server_default="not_requested"))
    op.add_column("evaluation_results", sa.Column("answer_relevance", sa.Float()))
    op.add_column("evaluation_results", sa.Column("answer_correctness", sa.Float()))
    op.add_column("evaluation_results", sa.Column("faithfulness", sa.Float()))
    op.add_column("evaluation_results", sa.Column("unsupported_claims", pg.JSONB(), nullable=False, server_default=sa.text("'[]'::jsonb")))


def downgrade():
    for name in ("unsupported_claims", "faithfulness", "answer_correctness", "answer_relevance", "generation_status", "prompt_version", "generation_model", "generated_answer"):
        op.drop_column("evaluation_results", name)
