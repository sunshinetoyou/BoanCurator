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
public final class SourceRepository_Factory implements Factory<SourceRepository> {
  private final Provider<ApiService> apiServiceProvider;

  public SourceRepository_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public SourceRepository get() {
    return newInstance(apiServiceProvider.get());
  }

  public static SourceRepository_Factory create(Provider<ApiService> apiServiceProvider) {
    return new SourceRepository_Factory(apiServiceProvider);
  }

  public static SourceRepository newInstance(ApiService apiService) {
    return new SourceRepository(apiService);
  }
}
