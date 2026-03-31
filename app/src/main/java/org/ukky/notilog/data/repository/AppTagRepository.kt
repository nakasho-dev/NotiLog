package org.ukky.notilog.data.repository

import kotlinx.coroutines.flow.Flow
import org.ukky.notilog.data.db.entity.AppTagEntity

interface AppTagRepository {
    fun getAll(): Flow<List<AppTagEntity>>
    fun getAllTags(): Flow<List<String>>
    suspend fun getByPackageName(packageName: String): AppTagEntity?
    suspend fun setTag(entity: AppTagEntity)
    suspend fun deleteTag(packageName: String)
    suspend fun getAllForBackup(): List<AppTagEntity>
}

