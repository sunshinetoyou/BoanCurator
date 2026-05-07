package com.boancurator.app.data.api

/**
 * 도메인별 sub-interface를 합성한 Retrofit 단일 진입점.
 * Repository는 이 interface를 주입받아 모든 엔드포인트를 호출할 수 있다.
 *
 * 각 도메인 interface는 같은 패키지의 별도 파일에 정의된다:
 * CardNewsApi, SearchApi, AuthApi, UserApi, BookmarkApi,
 * RatingApi, KeywordApi, NotificationApi, SourceApi, RecommendationApi
 */
interface ApiService :
    CardNewsApi,
    SearchApi,
    AuthApi,
    UserApi,
    BookmarkApi,
    RatingApi,
    KeywordApi,
    NotificationApi,
    SourceApi,
    RecommendationApi
