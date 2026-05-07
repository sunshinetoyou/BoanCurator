"""DB services 패키지 — 도메인별 모듈 + 호환성 re-export.

기존 호출자는 `from db import services; services.foo()` 또는
`from db.services import foo` 로 그대로 사용 가능.
"""
from .articles import (
    MAX_ANALYSIS_ATTEMPTS,
    is_article_exists,
    save_article,
    save_analysis,
    is_already_analyzed,
    get_article_by_url,
    get_next_article_to_analyze,
    record_analysis_failure,
    get_card_view_list,
    get_card_views_by_ids,
    search_articles_by_keyword,
    search_articles_by_any_themes,
    search_articles_by_all_themes,
    get_active_themes,
)
from .users import (
    get_or_create_user_by_google,
    get_user_by_id,
    _update_expertise_on_action,
    record_article_read,
    get_user_stats,
)
from .bookmarks import (
    create_bookmark,
    delete_bookmark,
    get_user_bookmarks,
)
from .ratings import (
    rate_article,
    get_user_ratings,
    delete_rating,
)
