package com.boancurator.app.di;

import com.boancurator.app.data.local.AppDatabase;
import com.boancurator.app.data.local.ArticleDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideArticleDaoFactory implements Factory<ArticleDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideArticleDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ArticleDao get() {
    return provideArticleDao(databaseProvider.get());
  }

  public static AppModule_ProvideArticleDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideArticleDaoFactory(databaseProvider);
  }

  public static ArticleDao provideArticleDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideArticleDao(database));
  }
}
