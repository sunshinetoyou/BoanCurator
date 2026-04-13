package com.boancurator.app.ui.screens.profile;

import com.boancurator.app.data.repository.AuthRepository;
import com.boancurator.app.data.repository.KeywordRepository;
import com.boancurator.app.data.repository.NotificationRepository;
import com.boancurator.app.data.repository.SourceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<KeywordRepository> keywordRepositoryProvider;

  private final Provider<NotificationRepository> notificationRepositoryProvider;

  private final Provider<SourceRepository> sourceRepositoryProvider;

  public ProfileViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<KeywordRepository> keywordRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider,
      Provider<SourceRepository> sourceRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.keywordRepositoryProvider = keywordRepositoryProvider;
    this.notificationRepositoryProvider = notificationRepositoryProvider;
    this.sourceRepositoryProvider = sourceRepositoryProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(authRepositoryProvider.get(), keywordRepositoryProvider.get(), notificationRepositoryProvider.get(), sourceRepositoryProvider.get());
  }

  public static ProfileViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<KeywordRepository> keywordRepositoryProvider,
      Provider<NotificationRepository> notificationRepositoryProvider,
      Provider<SourceRepository> sourceRepositoryProvider) {
    return new ProfileViewModel_Factory(authRepositoryProvider, keywordRepositoryProvider, notificationRepositoryProvider, sourceRepositoryProvider);
  }

  public static ProfileViewModel newInstance(AuthRepository authRepository,
      KeywordRepository keywordRepository, NotificationRepository notificationRepository,
      SourceRepository sourceRepository) {
    return new ProfileViewModel(authRepository, keywordRepository, notificationRepository, sourceRepository);
  }
}
