package com.example.comw.testutil

import com.example.comw.domain.logic.CollectionNameValidator
import com.example.comw.domain.repository.CollectionsRepository
import com.example.comw.domain.repository.NotesRepository
import com.example.comw.domain.repository.SearchHistoryRepository
import com.example.comw.domain.repository.SettingsRepository
import com.example.comw.model.AppSettings
import com.example.comw.model.CollectionDetails
import com.example.comw.model.CollectionSummary
import com.example.comw.model.NoteWithPerson
import com.example.comw.model.PersonNote
import com.example.comw.model.PersonalStatus
import com.example.comw.model.SortOrder
import com.example.comw.model.ThemeMode
import com.example.comw.model.TvmPerson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeNotesRepository(
    private val clock: FakeTimeProvider = FakeTimeProvider()
) : NotesRepository {

    val notes = MutableStateFlow<List<PersonNote>>(emptyList())

    fun setStatus(personId: Long, status: PersonalStatus, text: String = "") {
        notes.value = notes.value.filter { it.personId != personId } +
            PersonNote(personId, text, status, clock.now())
    }

    override fun observeNote(personId: Long): Flow<PersonNote?> =
        notes.map { list -> list.firstOrNull { it.personId == personId } }.distinctUntilChanged()

    override fun observeAllNotes(): Flow<List<PersonNote>> = notes

    override fun observeNotesWithPeople(): Flow<List<NoteWithPerson>> =
        notes.map { list -> list.map { NoteWithPerson(it, personName = null, personImageUrl = null) } }

    override suspend fun save(person: TvmPerson, text: String, status: PersonalStatus) {
        val rest = notes.value.filter { it.personId != person.id }
        notes.value = if (text.isBlank() && status == PersonalStatus.NONE) rest
        else rest + PersonNote(person.id, text.trim(), status, clock.now())
    }

    override suspend fun delete(personId: Long) {
        notes.value = notes.value.filter { it.personId != personId }
    }

    override suspend fun notedPersonIds(): List<Long> = notes.value.map { it.personId }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {

    val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state

    override suspend fun setCacheTtlMinutes(minutes: Int) { state.value = state.value.copy(cacheTtlMinutes = minutes) }
    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) { state.value = state.value.copy(backgroundSyncEnabled = enabled) }
    override suspend fun setSyncOnlyOnWifi(enabled: Boolean) { state.value = state.value.copy(syncOnlyOnWifi = enabled) }
    override suspend fun setShowHidden(show: Boolean) { state.value = state.value.copy(showHidden = show) }
    override suspend fun setSortOrder(order: SortOrder) { state.value = state.value.copy(sortOrder = order) }
    override suspend fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
    override suspend fun setLastSyncAt(time: Long) { state.value = state.value.copy(lastSyncAt = time) }
}

class FakeSearchHistoryRepository : SearchHistoryRepository {
    val recorded = MutableStateFlow<List<String>>(emptyList())

    override fun observeRecent(limit: Int): Flow<List<String>> = recorded
    override suspend fun record(query: String) {
        recorded.value = listOf(query) + recorded.value.filter { it != query }
    }
    override suspend fun clear() {
        recorded.value = emptyList()
    }
}

class FakeCollectionsRepository : CollectionsRepository {
    private var nextId = 1L
    val collections = MutableStateFlow<List<CollectionSummary>>(emptyList())
    /** id подборки -> id людей в ней */
    val members = MutableStateFlow<Map<Long, Set<Long>>>(emptyMap())

    override fun observeCollections(): Flow<List<CollectionSummary>> =
        combine(collections, members) { list, m -> list.map { it.copy(itemCount = m[it.id]?.size ?: 0) } }

    override fun observeCollection(id: Long): Flow<CollectionDetails?> = flowOf(null)

    override fun observeCollectionIdsFor(personId: Long): Flow<Set<Long>> =
        members.map { m -> m.filterValues { personId in it }.keys }

    override suspend fun create(name: String, description: String): Result<Long> {
        CollectionNameValidator.validate(name, collections.value.map { it.name })
            ?.let { return Result.failure(IllegalArgumentException(it)) }
        val id = nextId++
        collections.value = collections.value + CollectionSummary(id, name.trim(), description, 0L, 0)
        return Result.success(id)
    }

    override suspend fun update(id: Long, name: String, description: String): Result<Unit> = Result.success(Unit)

    override suspend fun delete(id: Long) {
        collections.value = collections.value.filter { it.id != id }
        members.value = members.value - id
    }

    override suspend fun addPerson(collectionId: Long, person: TvmPerson): Boolean {
        val current = members.value[collectionId] ?: emptySet()
        if (person.id in current) return false
        members.value = members.value + (collectionId to current + person.id)
        return true
    }

    override suspend fun removePerson(collectionId: Long, personId: Long) {
        val current = members.value[collectionId] ?: return
        members.value = members.value + (collectionId to current - personId)
    }

    override suspend fun allMemberIds(): List<Long> = members.value.values.flatten().distinct()
}
