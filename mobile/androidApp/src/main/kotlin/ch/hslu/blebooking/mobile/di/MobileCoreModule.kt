package ch.hslu.blebooking.mobile.di

import android.content.Context
import ch.hslu.blebooking.mobile.api.MobileCoreApi
import ch.hslu.blebooking.mobile.api.MobileCoreFactory
import ch.hslu.blebooking.mobile.storage.AndroidSecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class MobileCoreModule {

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): AndroidSecureStorage {
        return AndroidSecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideMobileCoreApi(
        storage: AndroidSecureStorage
    ): MobileCoreApi {
        return MobileCoreFactory.create(storage)
    }
}