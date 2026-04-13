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
public final class RatingRepository_Factory implements Factory<RatingRepository> {
  private final Provider<ApiService> apiServiceProvider;

  public RatingRepository_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public RatingRepository get() {
    return newInstance(apiServiceProvider.get());
  }

  public static RatingRepository_Factory create(Provider<ApiService> apiServiceProvider) {
    return new RatingRepository_Factory(apiServiceProvider);
  }

  public static RatingRepository newInstance(ApiService apiService) {
    return new RatingRepository(apiService);
  }
}
