package com.boancurator.app.ui.screens.bookmarks;

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
public final class BookmarksViewModel_Factory implements Factory<BookmarksViewModel> {
  private final Provider<BookmarkRepository> bookmarkRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public BookmarksViewModel_Factory(Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.bookmarkRepositoryProvider = bookmarkRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public BookmarksViewModel get() {
    return newInstance(bookmarkRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static BookmarksViewModel_Factory create(
      Provider<BookmarkRepository> bookmarkRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new BookmarksViewModel_Factory(bookmarkRepositoryProvider, authRepositoryProvider);
  }

  public static BookmarksViewModel newInstance(BookmarkRepository bookmarkRepository,
      AuthRepository authRepository) {
    return new BookmarksViewModel(bookmarkRepository, authRepository);
  }
}
