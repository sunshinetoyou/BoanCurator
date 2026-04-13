package com.boancurator.app.ui.screens.article;

import com.boancurator.app.data.repository.ArticleRepository;
import com.boancurator.app.data.repository.AuthRepository;
import com.boancurator.app.data.repository.BookmarkRepository;
import com.boancurator.app.data.repository.BookmarkStateHolder;
import com.boancurator.app.data.repository.RatingRepository;
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
public final class ArticleDetailViewModel_Factory implements Factory<ArticleDetailViewModel> {
  private final Provider<ArticleRepository> articleRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  private final Provider<RatingRepository> ratingRepositoryProvider;

  private final Provider<BookmarkStateHolder> bookmarkStateProvider;

  public ArticleDetailViewModel_Factory(Provider<ArticleRepository> articleRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<RatingRepository> ratingRepositoryProvider,
      Provider<BookmarkStateHolder> bookmarkStateProvider) {
    this.articleRepositoryProvider = articleRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
    this.ratingRepositoryProvider = ratingRepositoryProvider;
    this.bookmarkStateProvider = bookmarkStateProvider;
  }

  @Override
  public ArticleDetailViewModel get() {
    return newInstance(articleRepositoryProvider.get(), authRepositoryProvider.get(), bookmarkRepositoryProvider.get(), ratingRepositoryProvider.get(), bookmarkStateProvider.get());
  }

  public static ArticleDetailViewModel_Factory create(
      Provider<ArticleRepository> articleRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<RatingRepository> ratingRepositoryProvider,
      Provider<BookmarkStateHolder> bookmarkStateProvider) {
    return new ArticleDetailViewModel_Factory(articleRepositoryProvider, authRepositoryProvider, bookmarkRepositoryProvider, ratingRepositoryProvider, bookmarkStateProvider);
  }

  public static ArticleDetailViewModel newInstance(ArticleRepository articleRepository,
      AuthRepository authRepository, BookmarkRepository bookmarkRepository,
      RatingRepository ratingRepository, BookmarkStateHolder bookmarkState) {
    return new ArticleDetailViewModel(articleRepository, authRepository, bookmarkRepository, ratingRepository, bookmarkState);
  }
}
