package com.example.comw.unit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.comw.model.PersonalStatus
import com.example.comw.model.RefreshResult
import com.example.comw.navigation.Routes
import com.example.comw.testutil.FakeCollectionsRepository
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeHistoryRepository
import com.example.comw.testutil.FakeNotesRepository
import com.example.comw.testutil.FakePeopleRepository
import com.example.comw.testutil.MainDispatcherRule
import com.example.comw.testutil.TestData
import com.example.comw.ui.viewmodel.OFFLINE_NOTICE
import com.example.comw.ui.viewmodel.PeopleDetailViewModel
import com.example.comw.ui.viewmodel.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val people = FakePeopleRepository()
    private val favorites = FakeFavoritesRepository()
    private val history = FakeHistoryRepository()
    private val notes = FakeNotesRepository()
    private val collections = FakeCollectionsRepository()

    private fun createViewModel(id: Long) = PeopleDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.Detail.ARG_ID to id)),
        peopleRepository = people,
        favoritesRepository = favorites,
        historyRepository = history,
        notesRepository = notes,
        collectionsRepository = collections
    )

    /** uiState использует WhileSubscribed — "подписываемся", как это делает экран. */
    private fun TestScope.subscribe(vm: PeopleDetailViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `id из SavedStateHandle - обновляется именно этот человек с учётом TTL`() = runTest {
        createViewModel(id = 42)
        advanceUntilIdle()

        assertEquals(listOf(42L to false), people.refreshPersonCalls)
    }

    @Test(expected = IllegalStateException::class)
    fun `без id в SavedStateHandle ViewModel не создаётся`() {
        PeopleDetailViewModel(SavedStateHandle(), people, favorites, history, notes, collections)
    }

    @Test
    fun `офлайн с кэшем - детали из Room и плашка нет сети`() = runTest {
        people.putPerson(TestData.detail(7), fetchedAt = 100L)
        people.refreshPersonHandler = { _, _ -> RefreshResult.Failed(IOException()) }

        val vm = createViewModel(id = 7)
        subscribe(vm)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(UiState.Content(TestData.detail(7)), state.content)
        assertEquals(OFFLINE_NOTICE, state.notice)
        assertEquals(100L, state.fetchedAt)
    }

    @Test
    fun `офлайн без кэша - Error, Retry принудительно обновляет и показывает данные`() = runTest {
        people.refreshPersonHandler = { id, force ->
            if (!force) {
                RefreshResult.Failed(IOException())
            } else {
                people.putPerson(TestData.detail(id))
                RefreshResult.Updated
            }
        }
        val vm = createViewModel(id = 5)
        subscribe(vm)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.content is UiState.Error)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf(5L to false, 5L to true), people.refreshPersonCalls)
        assertEquals(UiState.Content(TestData.detail(5)), vm.uiState.value.content)
    }

    @Test
    fun `просмотр пишется в историю даже без сети, если человек есть в кэше`() = runTest {
        people.putPerson(TestData.detail(9, "Ivan"))
        people.refreshPersonHandler = { _, _ -> RefreshResult.Failed(IOException()) }

        createViewModel(id = 9)
        advanceUntilIdle()

        assertEquals(listOf(9L), history.recorded.value.map { it.person.id })
    }

    @Test
    fun `заметка и статус сохраняются и сразу видны в состоянии экрана`() = runTest {
        people.putPerson(TestData.detail(3))
        val vm = createViewModel(id = 3)
        subscribe(vm)
        advanceUntilIdle()

        vm.saveNote("  Посмотреть фильмографию ", PersonalStatus.TO_EXPLORE)
        advanceUntilIdle()

        val note = vm.uiState.value.note!!
        assertEquals("Посмотреть фильмографию", note.text)
        assertEquals(PersonalStatus.TO_EXPLORE, note.status)
    }

    @Test
    fun `новая подборка создаётся и человек сразу в неё добавляется, дубль имени - сообщение`() = runTest {
        people.putPerson(TestData.detail(4))
        val vm = createViewModel(id = 4)
        subscribe(vm)
        advanceUntilIdle()

        vm.messages.test {
            vm.createCollectionAndAdd("Актёры")
            assertEquals("Добавлено в «Актёры»", awaitItem())

            vm.createCollectionAndAdd(" актёры ")
            assertEquals("Подборка «актёры» уже есть", awaitItem())
        }

        val choice = vm.uiState.value.collections.single()
        assertEquals("Актёры", choice.collection.name)
        assertTrue(choice.containsPerson)
        assertEquals(1, choice.collection.itemCount)
    }
}
