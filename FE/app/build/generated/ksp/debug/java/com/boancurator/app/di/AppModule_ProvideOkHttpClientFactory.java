package com.boancurator.app.di;

import com.boancurator.app.data.api.AuthInterceptor;
import com.boancurator.app.data.api.TokenAuthenticator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class AppModule_ProvideOkHttpClientFactory implements Factory<OkHttpClient> {
  private final Provider<AuthInterceptor> authInterceptorProvider;

  private final Provider<TokenAuthenticator> tokenAuthenticatorProvider;

  public AppModule_ProvideOkHttpClientFactory(Provider<AuthInterceptor> authInterceptorProvider,
      Provider<TokenAuthenticator> tokenAuthenticatorProvider) {
    this.authInterceptorProvider = authInterceptorProvider;
    this.tokenAuthenticatorProvider = tokenAuthenticatorProvider;
  }

  @Override
  public OkHttpClient get() {
    return provideOkHttpClient(authInterceptorProvider.get(), tokenAuthenticatorProvider.get());
  }

  public static AppModule_ProvideOkHttpClientFactory create(
      Provider<AuthInterceptor> authInterceptorProvider,
      Provider<TokenAuthenticator> tokenAuthenticatorProvider) {
    return new AppModule_ProvideOkHttpClientFactory(authInterceptorProvider, tokenAuthenticatorProvider);
  }

  public static OkHttpClient provideOkHttpClient(AuthInterceptor authInterceptor,
      TokenAuthenticator tokenAuthenticator) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideOkHttpClient(authInterceptor, tokenAuthenticator));
  }
}
