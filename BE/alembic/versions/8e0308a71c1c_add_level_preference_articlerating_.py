"""add level_preference articlerating customsource

Revision ID: 8e0308a71c1c
Revises: 4cc899b1d6ed
Create Date: 2026-04-04
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


revision: str = '8e0308a71c1c'
down_revision: Union[str, Sequence[str], None] = '4cc899b1d6ed'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # User: level_preference 컬럼 추가
    op.add_column('user', sa.Column('level_preference', sa.Float(), server_default='3.0', nullable=False))

    # Analysis: domain_scores 컬럼 추가 (이미 있을 수 있음)
    try:
        op.add_column('analysis', sa.Column('domain_scores', sa.JSON(), nullable=True))
    except Exception:
        pass

    # User: expertise JSON 컬럼 추가 (이미 있을 수 있음)
    try:
        op.add_column('user', sa.Column('expertise', sa.JSON(), nullable=True))
    except Exception:
        pass

    # CustomSource 테이블 생성
    op.create_table('customsource',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('url', sa.String(), nullable=False),
        sa.Column('source_name', sa.String(), nullable=False),
        sa.Column('content_selector', sa.String(), nullable=True),
        sa.Column('has_full_content', sa.Boolean(), server_default='true', nullable=False),
        sa.Column('period', sa.Integer(), server_default='10800', nullable=False),
        sa.Column('enabled', sa.Boolean(), server_default='true', nullable=False),
        sa.Column('last_error', sa.String(), nullable=True),
        sa.Column('last_scraped_at', sa.DateTime(), nullable=True),
        sa.Column('created_at', sa.DateTime(), nullable=False),
    )
    op.create_index('ix_customsource_url', 'customsource', ['url'], unique=True)

    # ArticleRating 테이블 생성
    op.create_table('articlerating',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('user.id'), nullable=False),
        sa.Column('article_id', sa.Integer(), sa.ForeignKey('article.id'), nullable=False),
        sa.Column('rating', sa.Integer(), nullable=False),
        sa.Column('created_at', sa.DateTime(), nullable=False),
    )
    op.create_index('ix_articlerating_user_id', 'articlerating', ['user_id'])
    op.create_index('ix_articlerating_article_id', 'articlerating', ['article_id'])

    # 기존 expertise_level 컬럼 삭제 (있으면)
    try:
        op.drop_column('user', 'expertise_level')
    except Exception:
        pass


def downgrade() -> None:
    op.drop_table('articlerating')
    op.drop_index('ix_customsource_url', 'customsource')
    op.drop_table('customsource')
    op.drop_column('user', 'level_preference')
