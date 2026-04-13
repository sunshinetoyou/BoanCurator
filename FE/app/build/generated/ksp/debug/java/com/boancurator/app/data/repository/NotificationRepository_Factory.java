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
public final class NotificationRepository_Factory implements Factory<NotificationRepository> {
  private final Provider<ApiService> apiServiceProvider;

  public NotificationRepository_Factory(Provider<ApiService> apiServiceProvider) {
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public NotificationRepository get() {
    return newInstance(apiServiceProvider.get());
  }

  public static NotificationRepository_Factory create(Provider<ApiService> apiServiceProvider) {
    return new NotificationRepository_Factory(apiServiceProvider);
  }

  public static NotificationRepository newInstance(ApiService apiService) {
    return new NotificationRepository(apiService);
  }
}
