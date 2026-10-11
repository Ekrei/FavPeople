package com.example.comw.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO кэша людей. Абстрактный класс (а не интерфейс), чтобы можно было
 * написать метод с @Transaction из нескольких запросов.
 */
@Dao
abstract class PeopleDao {

    // ---------- главный список ----------
    @Query(
        "SELECT people.* FROM people " +
            "INNER JOIN main_list ON people.id = main_list.personId " +
            "ORDER BY main_list.position"
    )
    abstract fun observeMainList(): Flow<List<PersonEntity>>

    @Query("SELECT MAX(fetchedAt) FROM main_list")
    abstract fun observeMainListFetchedAt(): Flow<Long?>

    @Query("SELECT MAX(fetchedAt) FROM main_list")
    abstract suspend fun mainListFetchedAt(): Long?

    @Query("DELETE FROM main_list")
    abstract suspend fun clearMainList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMainListEntries(entries: List<MainListEntryEntity>)

    /** Атомарно заменить главный список: либо всё записалось, либо ничего. */
    @Transaction
    open suspend fun replaceMainList(people: List<PersonEntity>, fetchedAt: Long) {
        upsertPeople(people)
        clearMainList()
        insertMainListEntries(
            people.mapIndexed { index, person ->
                MainListEntryEntity(position = index, personId = person.id, fetchedAt = fetchedAt)
            }
        )
    }

    // ---------- отдельные люди ----------
    @Upsert
    abstract suspend fun upsertPeople(people: List<PersonEntity>)

    @Upsert
    abstract suspend fun upsertPerson(person: PersonEntity)

    /** Вставить, только если человека ещё нет (не затирая более полные данные). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(person: PersonEntity): Long

    @Query("SELECT * FROM people WHERE id = :id")
    abstract fun observePerson(id: Long): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE id = :id")
    abstract suspend fun getPerson(id: Long): PersonEntity?

    /** Офлайн-поиск по уже сохранённым людям. */
    @Query("SELECT * FROM people WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
    abstract suspend fun searchLocal(query: String): List<PersonEntity>

    @Query("SELECT COUNT(*) FROM people")
    abstract fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM people")
    abstract suspend fun count(): Int

    /**
     * Удалить старые записи кэша, на которые ничто не ссылается.
     * Людей из избранного, подборок, заметок, истории и главного списка не трогаем.
     */
    @Query(
        "DELETE FROM people WHERE fetchedAt < :olderThan " +
            "AND id NOT IN (SELECT personId FROM main_list) " +
            "AND id NOT IN (SELECT personId FROM favorites) " +
            "AND id NOT IN (SELECT personId FROM collection_items) " +
            "AND id NOT IN (SELECT personId FROM person_notes) " +
            "AND id NOT IN (SELECT personId FROM view_history)"
    )
    abstract suspend fun pruneUnreferenced(olderThan: Long): Int
}
