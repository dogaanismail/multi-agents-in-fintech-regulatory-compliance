"""One decision experience and at most one counterfactual per payment, so a
redelivered fraud analysis request or officer verdict cannot duplicate training data.

Revision ID: 004
Revises: 003
Create Date: 2026-10-04
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "004"
down_revision: Union[str, None] = "003"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.execute(
        """
        DELETE
        FROM agent_replay_buffer duplicate_entry
            USING agent_replay_buffer kept_entry
        WHERE duplicate_entry.payment_id = kept_entry.payment_id
          AND (duplicate_entry.reward_source = 'counterfactual') = (kept_entry.reward_source = 'counterfactual')
          AND (duplicate_entry.created_at, duplicate_entry.id) < (kept_entry.created_at, kept_entry.id)
        """
    )
    op.create_index(
        "uq_agent_replay_buffer_decision_payment_id",
        "agent_replay_buffer",
        ["payment_id"],
        unique=True,
        postgresql_where=sa.text("reward_source <> 'counterfactual'"),
    )
    op.create_index(
        "uq_agent_replay_buffer_counterfactual_payment_id",
        "agent_replay_buffer",
        ["payment_id"],
        unique=True,
        postgresql_where=sa.text("reward_source = 'counterfactual'"),
    )


def downgrade() -> None:
    op.drop_index("uq_agent_replay_buffer_counterfactual_payment_id", table_name="agent_replay_buffer")
    op.drop_index("uq_agent_replay_buffer_decision_payment_id", table_name="agent_replay_buffer")
