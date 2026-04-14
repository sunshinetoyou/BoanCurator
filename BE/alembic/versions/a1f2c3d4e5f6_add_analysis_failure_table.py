"""add analysis_failure table

Revision ID: a1f2c3d4e5f6
Revises: d30ea5a95304
Create Date: 2026-04-14 21:50:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = 'a1f2c3d4e5f6'
down_revision: Union[str, Sequence[str], None] = 'd30ea5a95304'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'analysisfailure',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('article_id', sa.Integer(), nullable=False),
        sa.Column('attempt_count', sa.Integer(), nullable=False, server_default='1'),
        sa.Column('last_attempted_at', sa.DateTime(), nullable=False),
        sa.Column('last_error', sa.String(), nullable=True),
        sa.ForeignKeyConstraint(['article_id'], ['article.id']),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('article_id'),
    )
    op.create_index(
        'ix_analysisfailure_article_id',
        'analysisfailure',
        ['article_id'],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index('ix_analysisfailure_article_id', table_name='analysisfailure')
    op.drop_table('analysisfailure')
