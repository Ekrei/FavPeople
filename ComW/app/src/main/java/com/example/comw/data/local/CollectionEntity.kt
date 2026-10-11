package com.example.comw.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Подборка, которую создал пользователь. */
@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: Long
)

/**
 * Связь "многие-ко-многим": один человек может быть в нескольких подборках,
 * а в подборке — много людей. Составной первичный ключ (collectionId, personId)
 * не даёт добавить человека в одну подборку дважды.
 * ForeignKey + CASCADE: при удалении подборки её элементы удаляются автоматически.
 */
@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "personId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("collectionId"), Index("personId")]
)
data class CollectionItemEntity(
    val collectionId: Long,
    val personId: Long,
    val addedAt: Long
)

/** Строка списка подборок с количеством людей (результат GROUP BY). */
data class CollectionSummaryRow(
    val id: Long,
    val name: String,
    val description: String,
    val createdAt: Long,
    val itemCount: Int
)
