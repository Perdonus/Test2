package com.perdonus.r34viewer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSearchDao {
    @Query("SELECT * FROM saved_searches ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedSearchEntity>>

    @Query("SELECT * FROM saved_searches WHERE serviceId = :serviceId ORDER BY createdAt DESC")
    fun observeAll(serviceId: String): Flow<List<SavedSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(search: SavedSearchEntity): Long

    @Query("UPDATE saved_searches SET label = :label WHERE id = :id")
    suspend fun rename(id: Long, label: String)

    @Query("DELETE FROM saved_searches WHERE id = :id")
    suspend fun delete(id: Long)
}
