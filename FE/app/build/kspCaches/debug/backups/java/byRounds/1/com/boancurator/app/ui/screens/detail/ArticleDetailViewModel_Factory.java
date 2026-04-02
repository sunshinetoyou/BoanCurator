package com.boancurator.app.ui.screens.detail;

import androidx.lifecycle.SavedStateHandle;
import com.boancurator.app.data.repository.ArticleRepository;
import com.boancurator.app.data.repository.AuthRepository;
import com.boancurator.app.data.repository.BookmarkRepository;
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
  private final Provider<SavedStateHandle> savedStateHandleProvider;

  private final Provider<ArticleRepository> articleRepositoryProvider;

  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public ArticleDetailViewModel_Factory(Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ArticleRepository> articleRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.savedStateHandleProvider = savedStateHandleProvider;
    this.articleRepositoryProvider = articleRepositoryProvider;
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ArticleDetailViewModel get() {
    return newInstance(savedStateHandleProvider.get(), articleRepositoryProvider.get(), bookmarkRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static ArticleDetailViewModel_Factory create(
      Provider<SavedStateHandle> savedStateHandleProvider,
      Provider<ArticleRepository> articleRepositoryProvider,
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ArticleDetailViewModel_Factory(savedStateHandleProvider, articleRepositoryProvider, bookmarkRepositoryProvider, authRepositoryProvider);
  }

  public static ArticleDetailViewModel newInstance(SavedStateHandle savedStateHandle,
      ArticleRepository articleRepository, BookmarkRepository bookmarkRepository,
      AuthRepository authRepository) {
    return new ArticleDetailViewModel(savedStateHandle, articleRepository, bookmarkRepository, authRepository);
  }
}
