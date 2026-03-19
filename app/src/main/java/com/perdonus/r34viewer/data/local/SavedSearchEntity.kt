package com.perdonus.r34viewer.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.perdonus.r34viewer.data.model.BooruService

@Entity(
    tableName = "saved_searches",
    indices = [Index(value = ["serviceId", "query"], unique = true)],
)
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceId: String,
    val query: String,
    val label: String,
    val createdAt: Long,
) {
    @Ignore
    val service: BooruService
        get() = BooruService.fromId(serviceId)
}
