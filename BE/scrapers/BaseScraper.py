import hashlib
import logging
import os
import re
from urllib.parse import urljoin, urlparse

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
_VALID_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"}


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

    # ── 이미지 수집 ──

    def _extract_image_urls(self, html: str, base_url: str = "") -> List[str]:
        """HTML에서 유효한 이미지 URL 목록을 추출합니다."""
        soup = BeautifulSoup(html, 'html.parser')
        urls = []

        for img in soup.find_all('img'):
            src = img.get('src', '').strip()
            if not src:
                continue

            # 상대 경로 → 절대 경로 변환
            if base_url and not src.startswith(('http://', 'https://')):
                src = urljoin(base_url, src)

            if not src.startswith(('http://', 'https://')):
                continue

            # 노이즈 URL 필터링
            if any(re.search(p, src, re.IGNORECASE) for p in _NOISE_URL_PATTERNS):
                continue

            # 너무 작은 이미지 필터링 (width/height 속성 기반)
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

    def _download_images(self, image_urls: List[str], article_id: int) -> List[str]:
        """이미지를 다운로드하여 로컬에 저장하고, 저장된 경로 목록을 반환합니다."""
        if not image_urls:
            return []

        save_dir = os.path.join(settings.images_dir, str(article_id))
        os.makedirs(save_dir, exist_ok=True)

        saved_paths = []
        max_size = settings.max_image_size_mb * 1024 * 1024

        for url in image_urls:
            try:
                res = requests.get(url, headers=self.headers, timeout=5, stream=True)
                res.raise_for_status()

                content_type = res.headers.get('Content-Type', '')
                if not content_type.startswith('image/'):
                    continue

                # 크기 확인
                content_length = res.headers.get('Content-Length')
                if content_length and int(content_length) > max_size:
                    continue

                data = res.content
                if len(data) > max_size:
                    continue

                # 확장자 결정
                parsed = urlparse(url)
                ext = os.path.splitext(parsed.path)[1].lower()
                if ext not in _VALID_EXTENSIONS:
                    ext = '.' + content_type.split('/')[-1].split(';')[0]
                    if ext not in _VALID_EXTENSIONS:
                        ext = '.jpg'

                # 파일명: URL 해시
                url_hash = hashlib.md5(url.encode()).hexdigest()[:12]
                filename = f"{url_hash}{ext}"
                filepath = os.path.join(save_dir, filename)

                with open(filepath, 'wb') as f:
                    f.write(data)

                saved_paths.append(filepath)
                logger.debug(f"이미지 저장: {filepath}")

            except Exception as e:
                logger.debug(f"이미지 다운로드 실패 ({url}): {e}")
                continue

        return saved_paths

    # ── 텍스트 정제 ──

    def _common_clean(self, text: str) -> str:
        """모든 스크래퍼가 공통으로 사용하는 고도화된 태그 정제 로직"""
        soup = BeautifulSoup(text, 'html.parser')

        if not soup: return ""

        # 1. 공통 노이즈 제거
        for s in soup(['script', 'style', 'iframe', 'noscript', 'header', 'footer', 'nav', 'form']):
            s.decompose()

        # 2. 표(Table) -> 마크다운 변환
        for table in soup.find_all('table'):
            markdown_table = self._html_table_to_markdown(table)
            table.replace_with(f"\n{markdown_table}\n")

        # 3. 이미지 -> [이미지: alt(src)] 변환
        for img in soup.find_all('img'):
            alt = img.get('alt', 'image')
            src = img.get('src', '')
            img.replace_with(f" [이미지: {alt}({src})] ")

        # 4. 첨부파일 링크 보존 (정규식 활용)
        for a in soup.find_all('a'):
            href = a.get('href', '')
            text = a.get_text(strip=True)
            if re.search(r'\.(pdf|zip|docx|xlsx|pptx|hwp)$', href.lower()):
                a.replace_with(f" [첨부파일: {text}({href})] ")
            else:
                a.replace_with(text) # 일반 링크는 텍스트만 남김

        # 5. 최종 텍스트 추출 (줄바꿈 보존)
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

    # 날짜 처리 로직
    """
        Pydantic은 기본적으로 ISO 8601 형식을 기대하기에,
        ex> 2025-12-24T03:28:18
        RSS 표준 표기 형식 RFC2822를 처리하지 못함
        ex> 'Wed, 24 Dec 2025 03:28:18 GMT'
        이를 해결하고자 날짜 처리 로직을 추가함.
    """
    def _get_date(self, raw_date):
        if not raw_date:
            return None
        return parser.parse(raw_date)
