package org.ukky.notilog.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.ukky.notilog.data.db.entity.AppTagEntity

@Dao
interface AppTagDao {

    @Query("SELECT * FROM app_tags ORDER BY tag")
    fun getAll(): Flow<List<AppTagEntity>>

    @Query("SELECT DISTINCT tag FROM app_tags ORDER BY tag")
    fun getAllTags(): Flow<List<String>>

    @Query("SELECT * FROM app_tags WHERE package_name = :packageName")
    suspend fun getByPackageName(packageName: String): AppTagEntity?

    @Upsert
    suspend fun upsert(entity: AppTagEntity)

    @Query("DELETE FROM app_tags WHERE package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT * FROM app_tags ORDER BY tag")
    suspend fun getAllForBackup(): List<AppTagEntity>
}

