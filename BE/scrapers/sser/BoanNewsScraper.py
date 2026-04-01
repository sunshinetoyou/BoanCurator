from db.models import Article
from db.services import is_article_exists
from scrapers.BaseScraper import BaseScraper

from sqlmodel import Session
from typing import List
import feedparser


class BoanNewsScraper(BaseScraper):

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

            # 이미지 URL 추출 후 다운로드 (content 정제 전에 수행)
            image_urls = self._extract_image_urls(raw_html, base_url=entry.link)

            content = self._clean_body(raw_html)
            if not self._is_valid_content(content):
                continue

            # article_id를 아직 모르므로 URL 해시로 임시 디렉토리 사용
            import hashlib
            temp_id = hashlib.md5(entry.link.encode()).hexdigest()[:10]
            image_paths = self._download_images(image_urls, temp_id)

            results.append(Article(
                title=entry.title,
                url=entry.link,
                content=content,
                source="BoanNews",
                published_at=self._get_date(entry.get('published') or entry.get('updated')),
                image_paths=image_paths if image_paths else None,
            ))

        return results

    def _scrap_raw_html(self, url) -> str:
        """보안뉴스 본문 HTML을 가져옵니다."""
        soup = self._get_soup(url)
        if not soup:
            return ""
        main_div = soup.find('div', id='news_content')
        if not main_div:
            return ""
        return str(main_div)

    def _clean_body(self, raw_html: str) -> str:
        """보안뉴스 특화 정제 (이미지 캡션 보존 후 common_clean)"""
        from bs4 import BeautifulSoup
        soup = BeautifulSoup(raw_html, 'html.parser')

        for img_block in soup.find_all('div', id='news_image'):
            p_tag = img_block.find('p')
            caption = f"\n\n[이미지 설명: {p_tag.get_text().strip()}]\n\n" if p_tag else ""
            img_block.replace_with(caption)

        return self._common_clean(str(soup))
