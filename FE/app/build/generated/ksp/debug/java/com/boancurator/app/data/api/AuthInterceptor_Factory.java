package com.boancurator.app.data.api;

import com.boancurator.app.data.repository.AuthStateManager;
import com.boancurator.app.util.TokenManager;
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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<AuthStateManager> authStateManagerProvider;

  public AuthInterceptor_Factory(Provider<TokenManager> tokenManagerProvider,
      Provider<AuthStateManager> authStateManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.authStateManagerProvider = authStateManagerProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(tokenManagerProvider.get(), authStateManagerProvider.get());
  }

  public static AuthInterceptor_Factory create(Provider<TokenManager> tokenManagerProvider,
      Provider<AuthStateManager> authStateManagerProvider) {
    return new AuthInterceptor_Factory(tokenManagerProvider, authStateManagerProvider);
  }

  public static AuthInterceptor newInstance(TokenManager tokenManager,
      AuthStateManager authStateManager) {
    return new AuthInterceptor(tokenManager, authStateManager);
  }
}
