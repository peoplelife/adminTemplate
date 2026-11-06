"""Record actual generation context and judge outcome."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql as pg

revision = "0005_eval_generation_details"
down_revision = "0004_add_generation_evaluation"
branch_labels = None
depends_on = None


def upgrade():
    op.add_column("evaluation_results", sa.Column("generation_details", pg.JSONB(), nullable=False, server_default=sa.text("'{}'::jsonb")))


def downgrade():
    op.drop_column("evaluation_results", "generation_details")
