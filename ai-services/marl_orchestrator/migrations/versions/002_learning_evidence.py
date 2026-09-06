"""Learning evidence: officer feedback columns on agent_replay_buffer and
probe-set evaluation metrics on agent_training_runs.

Revision ID: 002
Revises: 001
Create Date: 2026-09-06
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op

revision: str = "002"
down_revision: Union[str, None] = "001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column(
        "agent_replay_buffer",
        sa.Column(
            "officer_decision", sa.String(length=20), nullable=True,
            comment="APPROVE | REJECT — set when a compliance officer reviews",
        ),
    )
    op.add_column(
        "agent_replay_buffer",
        sa.Column(
            "feedback_type", sa.String(length=30), nullable=True,
            comment="MANUAL_REVIEW | DECISION_OVERRIDE",
        ),
    )
    op.add_column(
        "agent_training_runs",
        sa.Column(
            "probe_agreement_rate", sa.Float(), nullable=True,
            comment="Fraction of officer-labelled states where the policy now agrees",
        ),
    )
    op.add_column(
        "agent_training_runs",
        sa.Column(
            "probe_count", sa.Integer(), nullable=True,
            comment="Number of officer-labelled probe states",
        ),
    )
    op.add_column(
        "agent_training_runs",
        sa.Column(
            "probe_avg_q_gap", sa.Float(), nullable=True,
            comment="Mean Q(correct action) - Q(other action) over the probe set",
        ),
    )


def downgrade() -> None:
    op.drop_column("agent_training_runs", "probe_avg_q_gap")
    op.drop_column("agent_training_runs", "probe_count")
    op.drop_column("agent_training_runs", "probe_agreement_rate")
    op.drop_column("agent_replay_buffer", "feedback_type")
    op.drop_column("agent_replay_buffer", "officer_decision")
