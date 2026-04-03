"""기존 분석 데이터의 level을 domain_scores 기반 가중 평균으로 일괄 재계산"""
from sqlmodel import Session
from db.connection import engine
from db.services import recalculate_all_levels

with Session(engine) as session:
    counts = recalculate_all_levels(session)
    print(f"재계산 완료: {counts}")
