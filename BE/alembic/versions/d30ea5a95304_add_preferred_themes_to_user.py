"""add preferred_themes to user

Revision ID: d30ea5a95304
Revises: cc9f91e74ebd
Create Date: 2026-04-04
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import ARRAY


revision: str = 'd30ea5a95304'
down_revision: Union[str, Sequence[str], None] = 'cc9f91e74ebd'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('user', sa.Column('preferred_themes', ARRAY(sa.String()), nullable=True))


def downgrade() -> None:
    op.drop_column('user', 'preferred_themes')
