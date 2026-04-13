package com.boancurator.app.data.fcm;

import com.boancurator.app.data.api.ApiService;
import com.boancurator.app.util.TokenManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BoanFcmService_MembersInjector implements MembersInjector<BoanFcmService> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public BoanFcmService_MembersInjector(Provider<ApiService> apiServiceProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  public static MembersInjector<BoanFcmService> create(Provider<ApiService> apiServiceProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new BoanFcmService_MembersInjector(apiServiceProvider, tokenManagerProvider);
  }

  @Override
  public void injectMembers(BoanFcmService instance) {
    injectApiService(instance, apiServiceProvider.get());
    injectTokenManager(instance, tokenManagerProvider.get());
  }

  @InjectedFieldSignature("com.boancurator.app.data.fcm.BoanFcmService.apiService")
  public static void injectApiService(BoanFcmService instance, ApiService apiService) {
    instance.apiService = apiService;
  }

  @InjectedFieldSignature("com.boancurator.app.data.fcm.BoanFcmService.tokenManager")
  public static void injectTokenManager(BoanFcmService instance, TokenManager tokenManager) {
    instance.tokenManager = tokenManager;
  }
}
