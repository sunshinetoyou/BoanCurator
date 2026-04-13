package com.boancurator.app.ui.screens.archive;

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
public final class ArchiveViewModel_Factory implements Factory<ArchiveViewModel> {
  private final Provider<ArticleRepository> articleRepositoryProvider;

  public ArchiveViewModel_Factory(Provider<ArticleRepository> articleRepositoryProvider) {
    this.articleRepositoryProvider = articleRepositoryProvider;
  }

  @Override
  public ArchiveViewModel get() {
    return newInstance(articleRepositoryProvider.get());
  }

  public static ArchiveViewModel_Factory create(
      Provider<ArticleRepository> articleRepositoryProvider) {
    return new ArchiveViewModel_Factory(articleRepositoryProvider);
  }

  public static ArchiveViewModel newInstance(ArticleRepository articleRepository) {
    return new ArchiveViewModel(articleRepository);
  }
}
