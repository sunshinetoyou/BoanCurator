package com.boancurator.app.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class BookmarkStateHolder_Factory implements Factory<BookmarkStateHolder> {
  @Override
  public BookmarkStateHolder get() {
    return newInstance();
  }

  public static BookmarkStateHolder_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BookmarkStateHolder newInstance() {
    return new BookmarkStateHolder();
  }

  private static final class InstanceHolder {
    private static final BookmarkStateHolder_Factory INSTANCE = new BookmarkStateHolder_Factory();
  }
}
