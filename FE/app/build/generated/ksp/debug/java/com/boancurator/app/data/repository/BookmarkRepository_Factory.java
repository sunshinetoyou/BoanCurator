package com.boancurator.app.data.repository;

import com.boancurator.app.data.api.ApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class BookmarkRepository_Factory implements Factory<BookmarkRepository> {
  private final Provider<ApiService> apiServiceProvider;

  public BookmarkRepository_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public BookmarkRepository get() {
    return newInstance(apiServiceProvider.get());
  }

  public static BookmarkRepository_Factory create(Provider<ApiService> apiServiceProvider) {
    return new BookmarkRepository_Factory(apiServiceProvider);
  }

  public static BookmarkRepository newInstance(ApiService apiService) {
    return new BookmarkRepository(apiService);
  }
}
