"""Review queue: decision reason codes, payment exposure context, and officer
notes on agent_replay_buffer.

Revision ID: 003
Revises: 002
Create Date: 2026-09-06
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "003"
down_revision: Union[str, None] = "002"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "agent_replay_buffer",
        sa.Column(
            "officer_notes", sa.Text(), nullable=True,
            comment="Free-text reason the compliance officer gave with their verdict",
        ),
    )
    op.add_column(
        "agent_replay_buffer",
        sa.Column(
            "amount", sa.Float(), nullable=True,
            comment="Payment amount, for exposure-based queue ordering",
        ),
    )
    op.add_column(
        "agent_replay_buffer",
        sa.Column("currency", sa.String(length=10), nullable=True),
    )
    op.add_column(
        "agent_replay_buffer",
        sa.Column(
            "decision_reasons", postgresql.JSONB(), nullable=True,
            comment="Structured reason codes explaining the decision",
        ),
    )


def downgrade() -> None:
    op.drop_column("agent_replay_buffer", "decision_reasons")
    op.drop_column("agent_replay_buffer", "currency")
    op.drop_column("agent_replay_buffer", "amount")
    op.drop_column("agent_replay_buffer", "officer_notes")
