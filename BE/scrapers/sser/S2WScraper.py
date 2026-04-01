import re
import unicodedata

from db.models import Article
from db.services import is_article_exists
from scrapers.BaseScraper import BaseScraper

from sqlmodel import Session
from typing import List
import feedparser


class S2WScraper(BaseScraper):

    def collect(self, session: Session) -> List[Article]:
        feed = feedparser.parse(self.url)
        results = []

        for entry in feed.entries:
            if is_article_exists(session, entry.link):
                continue

            content_html = ""
            if hasattr(entry, 'content') and entry.content:
                content_html = entry.content[0].value

            if not content_html:
                continue

            image_urls = self._extract_image_urls(content_html, base_url=entry.link)
            content = self._clean_html(content_html)

            if not self._is_valid_content(content):
                continue

            results.append(Article(
                title=entry.title,
                url=entry.link,
                content=content,
                source="S2W Talon",
                published_at=self._get_date(entry.get('published')),
                image_urls=image_urls if image_urls else None,
            ))

        return results

    def _clean_html(self, html_str: str) -> str:
        text = self._common_clean(html_str)
        if not text: return ""

        text = unicodedata.normalize("NFKD", text)
        text = re.sub(r'(?<=[a-zA-Z0-9])\n([a-zA-Z0-9가-힣\s.,-]{1,20})\n(?=[a-zA-Z0-9])', r' \1 ', text)
        text = re.sub(r'\n\s*:', ':', text)

        noise_patterns = [
            r"\[이미지: \(https://medium\.com/.*stat\?event=.*\)\]",
            r"originally published in.*on Medium.*",
            r"Photo by .* on Unsplash",
        ]
        for pattern in noise_patterns:
            text = re.sub(pattern, "", text, flags=re.IGNORECASE | re.MULTILINE)

        text = re.sub(r'\n\s*\n+', '\n\n', text)
        return text.strip()[:self.MAX_CONTENT_LENGTH]
