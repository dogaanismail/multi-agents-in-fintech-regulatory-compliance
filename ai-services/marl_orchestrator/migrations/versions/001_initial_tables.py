"""Initial tables: agent_replay_buffer and agent_training_runs

Revision ID: 001
Revises:
Create Date: 2026-02-22

NOTE: If tables already exist (created via init_db / SQLAlchemy create_all),
      run `alembic stamp 001` to mark this migration as applied without
      re-executing it.
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ── agent_replay_buffer ───────────────────────────────────────────────────
    op.create_table(
        "agent_replay_buffer",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("payment_id", sa.String(255), nullable=False),
        sa.Column("state", postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column("actions", postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column("next_state", postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column("done", sa.Boolean(), nullable=False),
        sa.Column("automated_reward", sa.Float(), nullable=False),
        sa.Column("manual_reward", sa.Float(), nullable=True),
        sa.Column("effective_reward", sa.Float(), nullable=False),
        sa.Column("reward_source", sa.String(50), nullable=False),
        sa.Column("marl_action", sa.String(20), nullable=False),
        sa.Column("marl_confidence", sa.Float(), nullable=False),
        sa.Column("marl_q_value", sa.Float(), nullable=False),
        sa.Column("mean_risk_score", sa.Float(), nullable=False),
        sa.Column("is_used_in_training", sa.Boolean(), nullable=False),
        sa.Column("training_run_id", postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
    )
    op.create_index(
        "ix_agent_replay_buffer_payment_id",
        "agent_replay_buffer", ["payment_id"]
    )
    op.create_index(
        "ix_agent_replay_buffer_is_used_in_training",
        "agent_replay_buffer", ["is_used_in_training"]
    )

    # ── agent_training_runs ───────────────────────────────────────────────────
    op.create_table(
        "agent_training_runs",
        sa.Column("id", postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column("status", sa.String(20), nullable=False),
        sa.Column("experiences_count", sa.Integer(), nullable=False),
        sa.Column("train_steps_completed", sa.Integer(), nullable=False),
        sa.Column("batch_size", sa.Integer(), nullable=False),
        sa.Column("critic_loss", sa.Float(), nullable=True),
        sa.Column("actor_transaction_loss", sa.Float(), nullable=True),
        sa.Column("actor_customer_loss", sa.Float(), nullable=True),
        sa.Column("actor_network_loss", sa.Float(), nullable=True),
        sa.Column("model_saved", sa.Boolean(), nullable=False),
        sa.Column("error_message", sa.Text(), nullable=True),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
    )


def downgrade() -> None:
    op.drop_table("agent_training_runs")
    op.drop_index("ix_agent_replay_buffer_is_used_in_training", "agent_replay_buffer")
    op.drop_index("ix_agent_replay_buffer_payment_id", "agent_replay_buffer")
    op.drop_table("agent_replay_buffer")
