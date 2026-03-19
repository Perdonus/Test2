package com.perdonus.r34viewer.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePostDao {
    @Query("SELECT * FROM favorite_posts ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<FavoritePostEntity>>

    @Query("SELECT id FROM favorite_posts")
    fun observeIds(): Flow<List<Int>>

    @Query("SELECT * FROM favorite_posts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): FavoritePostEntity?

    @Upsert
    suspend fun upsert(post: FavoritePostEntity)

    @Query("DELETE FROM favorite_posts WHERE id = :id")
    suspend fun deleteById(id: Int)
}
