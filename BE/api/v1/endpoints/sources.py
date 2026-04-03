from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlmodel import Session, select
from typing import Optional

import feedparser
import requests
from bs4 import BeautifulSoup

from db.connection import get_session
from db.models import CustomSource

router = APIRouter()


class SourceCreateRequest(BaseModel):
    url: str
    source_name: Optional[str] = None
    content_selector: Optional[str] = None
    has_full_content: bool = True
    period: int = 10800


class SourceUpdateRequest(BaseModel):
    source_name: Optional[str] = None
    content_selector: Optional[str] = None
    has_full_content: Optional[bool] = None
    period: Optional[int] = None
    enabled: Optional[bool] = None


def _detect_rss_feed(url: str) -> dict | None:
    """URL에서 RSS 피드를 탐지. 직접 RSS이면 파싱, 아니면 HTML에서 link 태그 탐색."""
    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}

    # 1. 직접 RSS인지 확인
    feed = feedparser.parse(url)
    if feed.entries:
        return {
            "feed_url": url,
            "title": feed.feed.get("title", ""),
            "sample_count": len(feed.entries),
            "sample_title": feed.entries[0].get("title", ""),
        }

    # 2. HTML에서 RSS 링크 탐지
    try:
        resp = requests.get(url, timeout=10, headers=headers)
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")
        rss_links = soup.find_all(
            "link", attrs={"type": ["application/rss+xml", "application/atom+xml"]}
        )
        if not rss_links:
            return None

        rss_url = rss_links[0].get("href", "")
        if rss_url.startswith("/"):
            from urllib.parse import urljoin
            rss_url = urljoin(url, rss_url)

        feed = feedparser.parse(rss_url)
        if feed.entries:
            return {
                "feed_url": rss_url,
                "title": feed.feed.get("title", ""),
                "sample_count": len(feed.entries),
                "sample_title": feed.entries[0].get("title", ""),
            }
    except Exception:
        pass

    return None


@router.post("/sources/test")
def test_source(url: str = Query(..., description="테스트할 URL")):
    """URL의 RSS 피드 유효성 테스트 → 탐지 결과 + 샘플 기사 반환"""
    result = _detect_rss_feed(url)
    if not result:
        raise HTTPException(status_code=400, detail="RSS 피드를 찾을 수 없습니다")
    return result


@router.post("/sources")
def create_source(
    req: SourceCreateRequest,
    session: Session = Depends(get_session),
):
    """커스텀 소스 등록 (RSS 자동 탐지)"""
    # 중복 체크
    existing = session.exec(
        select(CustomSource).where(CustomSource.url == req.url)
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="이미 등록된 URL입니다")

    # RSS 탐지
    rss_info = _detect_rss_feed(req.url)
    if not rss_info:
        raise HTTPException(status_code=400, detail="RSS 피드를 찾을 수 없습니다")

    source = CustomSource(
        url=rss_info["feed_url"],
        source_name=req.source_name or rss_info.get("title", req.url),
        content_selector=req.content_selector,
        has_full_content=req.has_full_content,
        period=req.period,
    )
    session.add(source)
    session.commit()
    session.refresh(source)
    return source


@router.get("/sources")
def list_sources(session: Session = Depends(get_session)):
    """시스템 소스 + 커스텀 소스 전체 목록"""
    from scrapers.registry import get_system_scrapers

    # 시스템 소스
    system = [
        {"type": "system", "source_name": s.source_name, "url": s.url, "period": s.period}
        for s in get_system_scrapers()
    ]

    # 커스텀 소스
    custom_sources = session.exec(select(CustomSource)).all()
    custom = [
        {
            "type": "custom",
            "id": s.id,
            "source_name": s.source_name,
            "url": s.url,
            "period": s.period,
            "enabled": s.enabled,
            "last_error": s.last_error,
            "last_scraped_at": s.last_scraped_at,
        }
        for s in custom_sources
    ]

    return {"system": system, "custom": custom}


@router.put("/sources/{source_id}")
def update_source(
    source_id: int,
    req: SourceUpdateRequest,
    session: Session = Depends(get_session),
):
    """커스텀 소스 설정 수정"""
    source = session.get(CustomSource, source_id)
    if not source:
        raise HTTPException(status_code=404, detail="소스를 찾을 수 없습니다")

    if req.source_name is not None:
        source.source_name = req.source_name
    if req.content_selector is not None:
        source.content_selector = req.content_selector
    if req.has_full_content is not None:
        source.has_full_content = req.has_full_content
    if req.period is not None:
        source.period = req.period
    if req.enabled is not None:
        source.enabled = req.enabled

    session.add(source)
    session.commit()
    session.refresh(source)
    return source


@router.delete("/sources/{source_id}")
def delete_source(
    source_id: int,
    session: Session = Depends(get_session),
):
    """커스텀 소스 삭제"""
    source = session.get(CustomSource, source_id)
    if not source:
        raise HTTPException(status_code=404, detail="소스를 찾을 수 없습니다")
    session.delete(source)
    session.commit()
    return {"status": "deleted", "id": source_id}
