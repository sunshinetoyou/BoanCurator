package com.boancurator.app.ui.screens.home;

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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<ArticleRepository> articleRepositoryProvider;

  public HomeViewModel_Factory(Provider<ArticleRepository> articleRepositoryProvider) {
    this.articleRepositoryProvider = articleRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(articleRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<ArticleRepository> articleRepositoryProvider) {
    return new HomeViewModel_Factory(articleRepositoryProvider);
  }

  public static HomeViewModel newInstance(ArticleRepository articleRepository) {
    return new HomeViewModel(articleRepository);
  }
}
