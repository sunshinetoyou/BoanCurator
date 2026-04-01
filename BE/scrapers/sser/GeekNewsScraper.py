import hashlib

from db.models import Article
from db.services import is_article_exists
from scrapers.BaseScraper import BaseScraper

from sqlmodel import Session
from typing import List
import feedparser


class GeekNewsScraper(BaseScraper):

    def collect(self, session: Session) -> List[Article]:
        feed = feedparser.parse(self.url)

        ff = [
            entry for entry in feed.entries
            if not is_article_exists(session, entry.link)
        ]

        results = []
        for entry in ff:
            raw_html = self._scrap_raw_html(entry.link)
            if not raw_html:
                continue

            image_urls = self._extract_image_urls(raw_html, base_url=entry.link)
            content = self._common_clean(raw_html)
            if not self._is_valid_content(content):
                continue

            temp_id = hashlib.md5(entry.link.encode()).hexdigest()[:10]
            image_paths = self._download_images(image_urls, temp_id)

            results.append(Article(
                title=entry.title,
                url=entry.link,
                content=content,
                source="GeekNews",
                published_at=self._get_date(entry.get('published')),
                image_paths=image_paths if image_paths else None,
            ))

        return results

    def _scrap_raw_html(self, url) -> str:
        """긱뉴스 본문 HTML을 가져옵니다."""
        soup = self._get_soup(url)
        if not soup:
            return ""

        main_content = soup.select_one('#topic_contents')
        if not main_content:
            main_content = soup.select_one('.topic_contents')
        if not main_content:
            return ""

        return str(main_content)
