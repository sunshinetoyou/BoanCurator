package com.boancurator.app.data.repository;

import com.boancurator.app.data.api.ApiService;
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
public final class AuthRepository_Factory implements Factory<AuthRepository> {
  private final Provider<ApiService> apiServiceProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<AuthStateManager> authStateManagerProvider;

  private final Provider<BookmarkStateHolder> bookmarkStateHolderProvider;

  public AuthRepository_Factory(Provider<ApiService> apiServiceProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<AuthStateManager> authStateManagerProvider,
      Provider<BookmarkStateHolder> bookmarkStateHolderProvider) {
    this.apiServiceProvider = apiServiceProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.authStateManagerProvider = authStateManagerProvider;
    this.bookmarkStateHolderProvider = bookmarkStateHolderProvider;
  }

  @Override
  public AuthRepository get() {
    return newInstance(apiServiceProvider.get(), tokenManagerProvider.get(), authStateManagerProvider.get(), bookmarkStateHolderProvider.get());
  }

  public static AuthRepository_Factory create(Provider<ApiService> apiServiceProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<AuthStateManager> authStateManagerProvider,
      Provider<BookmarkStateHolder> bookmarkStateHolderProvider) {
    return new AuthRepository_Factory(apiServiceProvider, tokenManagerProvider, authStateManagerProvider, bookmarkStateHolderProvider);
  }

  public static AuthRepository newInstance(ApiService apiService, TokenManager tokenManager,
      AuthStateManager authStateManager, BookmarkStateHolder bookmarkStateHolder) {
    return new AuthRepository(apiService, tokenManager, authStateManager, bookmarkStateHolder);
  }
}
