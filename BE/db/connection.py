import logging
from sqlmodel import create_engine, Session, SQLModel
from alembic.config import Config
from alembic import command
from config import settings

logger = logging.getLogger(__name__)

engine = create_engine(settings.database_url, echo=False, pool_pre_ping=True)

def init_db():
    """Alembic 마이그레이션으로 DB 스키마 관리"""
    try:
        alembic_cfg = Config("alembic.ini")
        command.upgrade(alembic_cfg, "head")
        logger.info("Alembic 마이그레이션 완료")
    except Exception as e:
        logger.warning(f"Alembic 마이그레이션 실패, create_all 폴백: {e}")
        SQLModel.metadata.create_all(engine)

def get_session():
    """세션을 안전하게 가져오기 위한 Generator"""
    with Session(engine) as session:
        yield session