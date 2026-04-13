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
public final class KeywordRepository_Factory implements Factory<KeywordRepository> {
  private final Provider<ApiService> apiServiceProvider;

  public KeywordRepository_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public KeywordRepository get() {
    return newInstance(apiServiceProvider.get());
  }

  public static KeywordRepository_Factory create(Provider<ApiService> apiServiceProvider) {
    return new KeywordRepository_Factory(apiServiceProvider);
  }

  public static KeywordRepository newInstance(ApiService apiService) {
    return new KeywordRepository(apiService);
  }
}
