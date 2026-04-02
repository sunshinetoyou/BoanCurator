package com.boancurator.app.data.repository;

import com.boancurator.app.data.api.ApiService;
import com.boancurator.app.data.local.ArticleDao;
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
public final class ArticleRepository_Factory implements Factory<ArticleRepository> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<ArticleDao> articleDaoProvider;

  public ArticleRepository_Factory(Provider<ApiService> apiServiceProvider,
      Provider<ArticleDao> articleDaoProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.articleDaoProvider = articleDaoProvider;
  }

  @Override
  public ArticleRepository get() {
    return newInstance(apiServiceProvider.get(), articleDaoProvider.get());
  }

  public static ArticleRepository_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<ArticleDao> articleDaoProvider) {
    return new ArticleRepository_Factory(apiServiceProvider, articleDaoProvider);
  }

  public static ArticleRepository newInstance(ApiService apiService, ArticleDao articleDao) {
    return new ArticleRepository(apiService, articleDao);
  }
}
