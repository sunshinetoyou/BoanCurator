import logging
import re
from urllib.parse import urljoin

from abc import ABC, abstractmethod
from bs4 import BeautifulSoup
from sqlmodel import Session
from dateutil import parser
from typing import List
import requests

from config import settings
from db.models import Article

logger = logging.getLogger(__name__)

# 이미지로 간주하지 않을 패턴
_NOISE_URL_PATTERNS = [
    r"stat\?event=",          # Medium 트래킹 픽셀
    r"pixel\.",               # 일반 트래킹 픽셀
    r"beacon\.",
    r"\.gif\?",               # 트래킹 GIF
    r"data:image",            # data URI
    r"gravatar\.com",         # 프로필 아바타
    r"logo",                  # 로고 이미지
    r"icon",                  # 아이콘
    r"badge",                 # 배지
    r"avatar",                # 아바타
]


class BaseScraper(ABC):
    MAX_CONTENT_LENGTH = 16000

    def __init__(self, url: str, period: int):
        self.url = url
        self.headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'}
        self.period = period

    @abstractmethod
    def collect(self, session: Session) -> List[Article]:
        """RSS 또는 크롤링을 통해 전문을 가져오는 추상 메서드"""
        pass

    def _is_valid_content(self, content: str) -> bool:
        """콘텐츠가 유효한지 검증 (빈 문자열 또는 너무 짧은 경우 거부)"""
        return bool(content) and len(content.strip()) >= 50

    def _get_soup(self, url: str):
        """공통 헬퍼 메서드: URL에 접속하여 BeautifulSoup 객체 반환"""
        try:
            res = requests.get(url, headers=self.headers, timeout=10)
            res.raise_for_status()
            return BeautifulSoup(res.text, 'html.parser')
        except Exception as e:
            logger.error(f"Error fetching {url}: {e}")
            return None

    # ── 이미지 URL 추출 ──

    def _extract_image_urls(self, html: str, base_url: str = "") -> List[str]:
        """HTML에서 유효한 이미지 URL 목록을 추출합니다."""
        soup = BeautifulSoup(html, 'html.parser')
        urls = []

        for img in soup.find_all('img'):
            src = img.get('src', '').strip()
            if not src:
                continue

            if base_url and not src.startswith(('http://', 'https://')):
                src = urljoin(base_url, src)

            if not src.startswith(('http://', 'https://')):
                continue

            if any(re.search(p, src, re.IGNORECASE) for p in _NOISE_URL_PATTERNS):
                continue

            width = img.get('width', '')
            height = img.get('height', '')
            try:
                if width and int(width) < 100:
                    continue
                if height and int(height) < 100:
                    continue
            except ValueError:
                pass

            urls.append(src)

        return urls[:settings.max_images_per_article]

    # ── 텍스트 정제 ──

    def _common_clean(self, text: str) -> str:
        """모든 스크래퍼가 공통으로 사용하는 고도화된 태그 정제 로직"""
        soup = BeautifulSoup(text, 'html.parser')

        if not soup: return ""

        for s in soup(['script', 'style', 'iframe', 'noscript', 'header', 'footer', 'nav', 'form']):
            s.decompose()

        for table in soup.find_all('table'):
            markdown_table = self._html_table_to_markdown(table)
            table.replace_with(f"\n{markdown_table}\n")

        for img in soup.find_all('img'):
            alt = img.get('alt', 'image')
            src = img.get('src', '')
            img.replace_with(f" [이미지: {alt}({src})] ")

        for a in soup.find_all('a'):
            href = a.get('href', '')
            text = a.get_text(strip=True)
            if re.search(r'\.(pdf|zip|docx|xlsx|pptx|hwp)$', href.lower()):
                a.replace_with(f" [첨부파일: {text}({href})] ")
            else:
                a.replace_with(text)

        text = soup.get_text(separator='\n')
        text = re.sub(r'\n\s*\n+', '\n\n', text).strip()
        return text[:self.MAX_CONTENT_LENGTH]

    def _html_table_to_markdown(self, table) -> str:
        """HTML 표를 LLM이 읽기 좋은 Markdown 형식으로 변환"""
        rows = []
        for tr in table.find_all('tr'):
            cells = [td.get_text(strip=True) for td in tr.find_all(['th', 'td'])]
            if cells:
                rows.append("| " + " | ".join(cells) + " |")

        if not rows:
            return ""

        header_line = "| " + " | ".join(['---'] * len(rows[0].split('|')[1:-1])) + " |"
        if len(rows) > 1:
            rows.insert(1, header_line)

        return "\n".join(rows)

    def _get_date(self, raw_date):
        if not raw_date:
            return None
        return parser.parse(raw_date)
