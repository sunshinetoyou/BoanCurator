package com.boancurator.app.ui.screens.search;

import com.boancurator.app.data.repository.ArticleRepository;
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

  public SearchViewModel_Factory(Provider<ArticleRepository> articleRepositoryProvider) {
    this.articleRepositoryProvider = articleRepositoryProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(articleRepositoryProvider.get());
  }

  public static SearchViewModel_Factory create(
      Provider<ArticleRepository> articleRepositoryProvider) {
    return new SearchViewModel_Factory(articleRepositoryProvider);
  }

  public static SearchViewModel newInstance(ArticleRepository articleRepository) {
    return new SearchViewModel(articleRepository);
  }
}
