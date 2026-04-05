package org.ukky.notilog.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.ukky.notilog.data.db.DatabaseProvider
import org.ukky.notilog.data.db.NotiLogDatabase
import org.ukky.notilog.data.db.dao.AppTagDao
import org.ukky.notilog.data.db.dao.NotificationDao
import org.ukky.notilog.data.db.dao.NotificationRawLogDao
import org.ukky.notilog.data.repository.AppTagRepository
import org.ukky.notilog.data.repository.AppTagRepositoryImpl
import org.ukky.notilog.data.repository.NotificationRepository
import org.ukky.notilog.data.repository.NotificationRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotiLogDatabase =
        DatabaseProvider.getDatabase(context)

    @Provides
    fun provideNotificationDao(db: NotiLogDatabase): NotificationDao =
        db.notificationDao()

    @Provides
    fun provideAppTagDao(db: NotiLogDatabase): AppTagDao =
        db.appTagDao()

    @Provides
    fun provideNotificationRawLogDao(db: NotiLogDatabase): NotificationRawLogDao =
        db.notificationRawLogDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindAppTagRepository(impl: AppTagRepositoryImpl): AppTagRepository
}

