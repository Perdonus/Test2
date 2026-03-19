package com.perdonus.r34viewer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [FavoritePostEntity::class, SavedSearchEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritePostDao(): FavoritePostDao

    abstract fun savedSearchDao(): SavedSearchDao
}
