import hashlib

from db.models import Article
from db.services import is_article_exists
from scrapers.BaseScraper import BaseScraper

from sqlmodel import Session
from typing import List, Optional
import feedparser


class RSSGenericScraper(BaseScraper):
    """설정만으로 새 RSS 소스를 추가할 수 있는 범용 스크래퍼.

    Args:
        url: RSS 피드 URL
        period: 수집 주기 (초)
        source_name: DB에 저장할 소스 이름
        content_selector: 본문 CSS 셀렉터 (RSS에 전문 미포함 시 사용)
        has_full_content: True이면 RSS content:encoded에서 본문 추출 (추가 스크래핑 생략)
    """

    def __init__(
        self,
        url: str,
        period: int,
        source_name: str,
        content_selector: Optional[str] = None,
        has_full_content: bool = False,
    ):
        super().__init__(url, period)
        self.source_name = source_name
        self.content_selector = content_selector
        self.has_full_content = has_full_content

    def collect(self, session: Session) -> List[Article]:
        feed = feedparser.parse(self.url)
        results = []

        for entry in feed.entries:
            if is_article_exists(session, entry.link):
                continue

            raw_html = self._get_raw_html(entry)
            image_urls = self._extract_image_urls(raw_html, base_url=entry.link) if raw_html else []

            content = self._clean_content(raw_html, entry)
            if not self._is_valid_content(content):
                continue

            temp_id = hashlib.md5(entry.link.encode()).hexdigest()[:10]
            image_paths = self._download_images(image_urls, temp_id)

            results.append(Article(
                title=entry.title,
                url=entry.link,
                content=content,
                source=self.source_name,
                published_at=self._get_date(entry.get("published")),
                image_paths=image_paths if image_paths else None,
            ))

        return results

    def _get_raw_html(self, entry) -> str:
        """원본 HTML 가져오기 (이미지 추출용)"""
        if self.has_full_content:
            if hasattr(entry, "content") and entry.content and entry.content[0].value.strip():
                return entry.content[0].value
            if hasattr(entry, "summary") and entry.summary.strip():
                return entry.summary
            return ""

        if not self.content_selector:
            return ""

        soup = self._get_soup(entry.link)
        if not soup:
            return ""
        main_content = soup.select_one(self.content_selector)
        return str(main_content) if main_content else ""

    def _clean_content(self, raw_html: str, entry) -> str:
        """HTML 정제"""
        if not raw_html:
            return ""
        return self._common_clean(raw_html)
