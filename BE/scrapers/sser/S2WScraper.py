import hashlib
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

            # RSS 피드 내부에 전문이 있는지 확인 (스크래핑 X)
            content_html = ""
            if hasattr(entry, 'content') and entry.content:
                content_html = entry.content[0].value

            if not content_html:
                continue

            # 이미지 추출 (정제 전)
            image_urls = self._extract_image_urls(content_html, base_url=entry.link)
            content = self._clean_html(content_html)

            if not self._is_valid_content(content):
                continue

            temp_id = hashlib.md5(entry.link.encode()).hexdigest()[:10]
            image_paths = self._download_images(image_urls, temp_id)

            results.append(Article(
                title=entry.title,
                url=entry.link,
                content=content,
                source="S2W Talon",
                published_at=self._get_date(entry.get('published')),
                image_paths=image_paths if image_paths else None,
            ))

        return results

    def _clean_html(self, html_str: str) -> str:
        # 1. 공통 로직 실행
        text = self._common_clean(html_str)
        if not text: return ""

        # 2. 유니코드 정규화 (\xa0 제거 등)
        text = unicodedata.normalize("NFKD", text)

        # A. 문장 중간에 단어 하나만 두고 줄바꿈된 것 연결
        text = re.sub(r'(?<=[a-zA-Z0-9])\n([a-zA-Z0-9가-힣\s.,-]{1,20})\n(?=[a-zA-Z0-9])', r' \1 ', text)

        # B. 콜론(:) 앞의 줄바꿈 및 공백 제거
        text = re.sub(r'\n\s*:', ':', text)

        # C. 불필요한 노이즈 패턴 제거
        noise_patterns = [
            r"\[이미지: \(https://medium\.com/.*stat\?event=.*\)\]",
            r"originally published in.*on Medium.*",
            r"Photo by .* on Unsplash",
        ]
        for pattern in noise_patterns:
            text = re.sub(pattern, "", text, flags=re.IGNORECASE | re.MULTILINE)

        # 3. 최종 줄바꿈 정리
        text = re.sub(r'\n\s*\n+', '\n\n', text)
        return text.strip()[:self.MAX_CONTENT_LENGTH]
