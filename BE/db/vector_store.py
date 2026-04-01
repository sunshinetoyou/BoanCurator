import logging
from typing import Optional

import chromadb
import requests
from google import genai
from google.genai import types

from config import settings

logger = logging.getLogger(__name__)

_genai_client = genai.Client(api_key=settings.gemini_api_key)

_chroma_client = chromadb.PersistentClient(path="./chroma_data")
_collection = _chroma_client.get_or_create_collection(
    name="article_embeddings",
    metadata={"hnsw:space": "cosine"},
)

# MIME 타입 매핑
_MIME_MAP = {
    "image/jpeg": "image/jpeg",
    "image/png": "image/png",
    "image/gif": "image/gif",
    "image/webp": "image/webp",
}


def _download_image_bytes(url: str) -> tuple[bytes, str] | None:
    """이미지 URL에서 바이트를 다운로드합니다. (임시, 메모리에서만 사용)"""
    try:
        res = requests.get(url, timeout=5, headers={
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
        })
        res.raise_for_status()
        content_type = res.headers.get("Content-Type", "")
        mime = None
        for key in _MIME_MAP:
            if key in content_type:
                mime = _MIME_MAP[key]
                break
        if not mime:
            return None
        return res.content, mime
    except Exception:
        return None


def _embed_content(text: str, image_urls: list[str] = None) -> list[float]:
    """Gemini Embedding 모델로 임베딩 벡터 생성.
    이미지 URL이 있으면 임시 다운로드하여 멀티모달 임베딩합니다."""
    contents = [text]

    if image_urls:
        for url in image_urls[:5]:
            result = _download_image_bytes(url)
            if not result:
                continue
            data, mime = result
            contents.append(types.Part.from_bytes(data=data, mime_type=mime))

    result = _genai_client.models.embed_content(
        model=settings.embedding_model,
        contents=contents,
    )
    return result.embeddings[0].values


def store_embedding(
    article_id: int,
    title: str,
    summary: str,
    metadata: dict,
    image_urls: list[str] = None,
) -> None:
    """기사 임베딩을 ChromaDB에 저장합니다."""
    doc_id = str(article_id)

    existing = _collection.get(ids=[doc_id])
    if existing and existing["ids"]:
        return

    text_to_embed = f"{title}\n{summary}"
    try:
        embedding = _embed_content(text_to_embed, image_urls)
        _collection.add(
            ids=[doc_id],
            embeddings=[embedding],
            metadatas=[metadata],
            documents=[text_to_embed],
        )
        has_images = bool(image_urls)
        logger.info(f"임베딩 저장 완료: article_id={article_id} (멀티모달={has_images})")
    except Exception as e:
        logger.error(f"임베딩 저장 실패 (article_id={article_id}): {e}")


def search_similar(query_text: str, n_results: int = 10, where: dict = None) -> list[dict]:
    """쿼리 텍스트와 유사한 기사를 검색합니다."""
    try:
        query_embedding = _embed_content(query_text)
        results = _collection.query(
            query_embeddings=[query_embedding],
            n_results=n_results,
            where=where,
            include=["documents", "metadatas", "distances"],
        )

        items = []
        for i in range(len(results["ids"][0])):
            items.append({
                "article_id": int(results["ids"][0][i]),
                "document": results["documents"][0][i],
                "metadata": results["metadatas"][0][i],
                "distance": results["distances"][0][i],
            })
        return items
    except Exception as e:
        logger.error(f"유사도 검색 실패: {e}")
        return []


def search_similar_by_id(article_id: int, n_results: int = 10) -> list[dict]:
    """특정 기사와 유사한 기사를 검색합니다."""
    doc_id = str(article_id)
    existing = _collection.get(ids=[doc_id], include=["embeddings"])

    if not existing or not existing["ids"]:
        return []

    embedding = existing["embeddings"][0]
    results = _collection.query(
        query_embeddings=[embedding],
        n_results=n_results + 1,
        include=["documents", "metadatas", "distances"],
    )

    items = []
    for i in range(len(results["ids"][0])):
        aid = int(results["ids"][0][i])
        if aid == article_id:
            continue
        items.append({
            "article_id": aid,
            "document": results["documents"][0][i],
            "metadata": results["metadatas"][0][i],
            "distance": results["distances"][0][i],
        })
    return items[:n_results]


def batch_embed_existing(session) -> int:
    """DB에 분석 완료되었지만 ChromaDB에 없는 기사를 일괄 임베딩합니다."""
    from .models import Article, Analysis
    from sqlmodel import select

    statement = (
        select(Article, Analysis)
        .join(Analysis, Article.id == Analysis.article_id)
    )
    rows = session.exec(statement).all()

    count = 0
    for article, analysis in rows:
        doc_id = str(article.id)
        existing = _collection.get(ids=[doc_id])
        if existing and existing["ids"]:
            continue

        metadata = {
            "source": article.source,
            "category": analysis.category,
            "level": analysis.level,
        }
        store_embedding(
            article.id,
            article.title,
            analysis.summary,
            metadata,
            image_urls=article.image_urls,
        )
        count += 1

    logger.info(f"백필 완료: {count}건 임베딩 생성")
    return count
