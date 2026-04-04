"""add keyword notification tables

Revision ID: cc9f91e74ebd
Revises: 8e0308a71c1c
Create Date: 2026-04-04
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = 'cc9f91e74ebd'
down_revision: Union[str, Sequence[str], None] = '8e0308a71c1c'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table('keywordalert',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('user.id'), nullable=False),
        sa.Column('keyword', sa.String(), nullable=False),
        sa.Column('embedding_id', sa.String(), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False),
    )
    op.create_index('ix_keywordalert_user_id', 'keywordalert', ['user_id'])

    op.create_table('notificationlog',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('user.id'), nullable=False),
        sa.Column('article_id', sa.Integer(), sa.ForeignKey('article.id'), nullable=False),
        sa.Column('keyword_alert_id', sa.Integer(), sa.ForeignKey('keywordalert.id'), nullable=False),
        sa.Column('sent_at', sa.DateTime(), nullable=False),
    )
    op.create_index('ix_notificationlog_user_id', 'notificationlog', ['user_id'])

    op.create_table('usernotificationsettings',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('user.id'), nullable=False, unique=True),
        sa.Column('match_preset', sa.String(), server_default='normal', nullable=False),
        sa.Column('top_n', sa.Integer(), server_default='3', nullable=False),
        sa.Column('daily_limit', sa.Integer(), server_default='5', nullable=False),
        sa.Column('mode', sa.String(), server_default='realtime', nullable=False),
        sa.Column('fcm_token', sa.String(), nullable=True),
    )
    op.create_index('ix_usernotificationsettings_user_id', 'usernotificationsettings', ['user_id'])


def downgrade() -> None:
    op.drop_table('usernotificationsettings')
    op.drop_table('notificationlog')
    op.drop_table('keywordalert')
