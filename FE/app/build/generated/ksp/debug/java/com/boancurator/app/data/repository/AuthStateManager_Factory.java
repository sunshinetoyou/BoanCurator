package com.boancurator.app.data.repository;

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
public final class AuthStateManager_Factory implements Factory<AuthStateManager> {
  private final Provider<TokenManager> tokenManagerProvider;

  public AuthStateManager_Factory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public AuthStateManager get() {
    return newInstance(tokenManagerProvider.get());
  }

  public static AuthStateManager_Factory create(Provider<TokenManager> tokenManagerProvider) {
    return new AuthStateManager_Factory(tokenManagerProvider);
  }

  public static AuthStateManager newInstance(TokenManager tokenManager) {
    return new AuthStateManager(tokenManager);
  }
}
