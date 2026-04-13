package com.boancurator.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전체에서 공유되는 북마크 상태.
 * HomeViewModel과 BookmarksViewModel이 동일 인스턴스를 참조.
 */
@Singleton
class BookmarkStateHolder @Inject constructor() {

    // url -> bookmarkId
    private val _bookmarkMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val bookmarkMap: StateFlow<Map<String, Int>> = _bookmarkMap.asStateFlow()

    fun update(map: Map<String, Int>) {
        _bookmarkMap.value = map
    }

    fun add(url: String, bookmarkId: Int) {
        _bookmarkMap.value = _bookmarkMap.value + (url to bookmarkId)
    }

    fun remove(url: String) {
        _bookmarkMap.value = _bookmarkMap.value - url
    }

    fun isBookmarked(url: String): Boolean = url in _bookmarkMap.value

    fun getBookmarkId(url: String): Int? = _bookmarkMap.value[url]

    fun clear() {
        _bookmarkMap.value = emptyMap()
    }
}
