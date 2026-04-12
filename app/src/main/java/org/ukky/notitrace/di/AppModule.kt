package org.ukky.notitrace.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.ukky.notitrace.data.db.DatabaseProvider
import org.ukky.notitrace.data.db.NotiTraceDatabase
import org.ukky.notitrace.data.db.dao.AppTagDao
import org.ukky.notitrace.data.db.dao.NotificationDao
import org.ukky.notitrace.data.db.dao.NotificationRawLogDao
import org.ukky.notitrace.data.repository.AppTagRepository
import org.ukky.notitrace.data.repository.AppTagRepositoryImpl
import org.ukky.notitrace.data.repository.NotificationRepository
import org.ukky.notitrace.data.repository.NotificationRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotiTraceDatabase =
        DatabaseProvider.getDatabase(context)

    @Provides
    fun provideNotificationDao(db: NotiTraceDatabase): NotificationDao =
        db.notificationDao()

    @Provides
    fun provideAppTagDao(db: NotiTraceDatabase): AppTagDao =
        db.appTagDao()

    @Provides
    fun provideNotificationRawLogDao(db: NotiTraceDatabase): NotificationRawLogDao =
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

