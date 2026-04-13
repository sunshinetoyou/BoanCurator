package com.boancurator.app.ui.screens.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ArticleRepository> articleRepositoryProvider;

  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BookmarkStateHolder> bookmarkStateProvider;

  public HomeViewModel_Factory(Provider<ArticleRepository> articleRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<BookmarkStateHolder> bookmarkStateProvider) {
    this.articleRepositoryProvider = articleRepositoryProvider;
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.bookmarkStateProvider = bookmarkStateProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(articleRepositoryProvider.get(), bookmarkRepositoryProvider.get(), authRepositoryProvider.get(), bookmarkStateProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<ArticleRepository> articleRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<BookmarkStateHolder> bookmarkStateProvider) {
    return new HomeViewModel_Factory(articleRepositoryProvider, bookmarkRepositoryProvider, authRepositoryProvider, bookmarkStateProvider);
  }

  public static HomeViewModel newInstance(ArticleRepository articleRepository,
      BookmarkRepository bookmarkRepository, AuthRepository authRepository,
      BookmarkStateHolder bookmarkState) {
    return new HomeViewModel(articleRepository, bookmarkRepository, authRepository, bookmarkState);
  }
}
