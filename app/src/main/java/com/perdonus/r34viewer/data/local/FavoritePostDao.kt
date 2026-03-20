package com.perdonus.r34viewer.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePostDao {
    @Query("SELECT * FROM favorite_posts ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE serviceId = :serviceId ORDER BY savedAt DESC")
    fun observeAll(serviceId: String): Flow<List<FavoritePostEntity>>

    @Query("SELECT * FROM favorite_posts WHERE serviceId = :serviceId AND id = :id LIMIT 1")
    suspend fun getById(serviceId: String, id: String): FavoritePostEntity?

    @Upsert
    suspend fun upsert(post: FavoritePostEntity)

    @Query("DELETE FROM favorite_posts WHERE serviceId = :serviceId AND id = :id")
    suspend fun deleteById(serviceId: String, id: String)
}
