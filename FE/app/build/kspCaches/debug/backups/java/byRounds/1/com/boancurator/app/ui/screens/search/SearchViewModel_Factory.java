package com.boancurator.app.ui.screens.search;

import com.boancurator.app.data.repository.ArticleRepository;
import com.boancurator.app.data.repository.AuthRepository;
import com.boancurator.app.data.repository.BookmarkRepository;
import com.boancurator.app.data.repository.BookmarkStateHolder;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<ArticleRepository> articleRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  private final Provider<BookmarkStateHolder> bookmarkStateProvider;

  public SearchViewModel_Factory(Provider<ArticleRepository> articleRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<BookmarkStateHolder> bookmarkStateProvider) {
    this.articleRepositoryProvider = articleRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
    this.bookmarkStateProvider = bookmarkStateProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(articleRepositoryProvider.get(), authRepositoryProvider.get(), bookmarkRepositoryProvider.get(), bookmarkStateProvider.get());
  }

  public static SearchViewModel_Factory create(
      Provider<ArticleRepository> articleRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<BookmarkStateHolder> bookmarkStateProvider) {
    return new SearchViewModel_Factory(articleRepositoryProvider, authRepositoryProvider, bookmarkRepositoryProvider, bookmarkStateProvider);
  }

  public static SearchViewModel newInstance(ArticleRepository articleRepository,
      AuthRepository authRepository, BookmarkRepository bookmarkRepository,
      BookmarkStateHolder bookmarkState) {
    return new SearchViewModel(articleRepository, authRepository, bookmarkRepository, bookmarkState);
  }
}
