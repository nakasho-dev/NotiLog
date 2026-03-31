package org.ukky.notilog.data.repository

import kotlinx.coroutines.flow.Flow
import org.ukky.notilog.data.db.dao.AppTagDao
import org.ukky.notilog.data.db.entity.AppTagEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppTagRepositoryImpl @Inject constructor(
    private val dao: AppTagDao,
) : AppTagRepository {

    override fun getAll(): Flow<List<AppTagEntity>> = dao.getAll()

    override fun getAllTags(): Flow<List<String>> = dao.getAllTags()

    override suspend fun getByPackageName(packageName: String): AppTagEntity? =
        dao.getByPackageName(packageName)

    override suspend fun setTag(entity: AppTagEntity) = dao.upsert(entity)

    override suspend fun deleteTag(packageName: String) =
        dao.deleteByPackageName(packageName)

    override suspend fun getAllForBackup(): List<AppTagEntity> =
        dao.getAllForBackup()
}

