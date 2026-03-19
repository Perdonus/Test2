package com.perdonus.r34viewer.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_searches",
    indices = [Index(value = ["query"], unique = true)],
)
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val label: String,
    val createdAt: Long,
)
