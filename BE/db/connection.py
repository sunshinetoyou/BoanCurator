from sqlmodel import create_engine, Session, SQLModel
from config import settings

engine = create_engine(settings.database_url, echo=False, pool_pre_ping=True)

def init_db():
    """테이블이 없으면 생성하는 함수"""
    SQLModel.metadata.create_all(engine)

def get_session():
    """세션을 안전하게 가져오기 위한 Generator"""
    with Session(engine) as session:
        yield session