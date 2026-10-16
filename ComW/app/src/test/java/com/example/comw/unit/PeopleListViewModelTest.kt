package com.example.comw.unit

import app.cash.turbine.test
import com.example.comw.domain.logic.PersonRow
import com.example.comw.model.PersonalStatus
import com.example.comw.model.RefreshResult
import com.example.comw.model.SearchResult
import com.example.comw.model.TvmPerson
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeNotesRepository
import com.example.comw.testutil.FakePeopleRepository
import com.example.comw.testutil.FakeSearchHistoryRepository
import com.example.comw.testutil.FakeSettingsRepository
import com.example.comw.testutil.MainDispatcherRule
import com.example.comw.testutil.TestData
import com.example.comw.ui.viewmodel.OFFLINE_NOTICE
import com.example.comw.ui.viewmodel.OFFLINE_SEARCH_NOTICE
import com.example.comw.ui.viewmodel.PeopleListUiState
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.PeopleListViewModel.Companion.SEARCH_DEBOUNCE_MS
import com.example.comw.ui.viewmodel.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Реактивная/экранная логика главного экрана (финальная offline-first версия). */
@OptIn(ExperimentalCoroutinesApi::class)
class PeopleListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val people = FakePeopleRepository()
    private val favorites = FakeFavoritesRepository()
    private val notes = FakeNotesRepository()
    private val settings = FakeSettingsRepository()
    private val searchHistory = FakeSearchHistoryRepository()

    private fun createViewModel() =
        PeopleListViewModel(people, favorites, notes, settings, searchHistory)

    @Suppress("UNCHECKED_CAST")
    private fun PeopleListViewModel.ids(): List<Long> =
        (uiState.value.content as UiState.Content<List<PersonRow>>).data.map { it.person.id }

    @Test
    fun `начальное состояние - Loading`() = runTest {
        val vm = createViewModel()
        assertEquals(PeopleListUiState(), vm.uiState.value)
    }

    @Test
    fun `офлайн с кэшем - показываются сохранённые данные и плашка, а не ошибка`() = runTest {
        people.mainList.value = listOf(TestData.alice, TestData.bob)
        people.mainListUpdatedAt.value = 500L
        people.refreshMainListHandler = { RefreshResult.Failed(IOException()) }

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf(1L, 2L), vm.ids())
        assertEquals(OFFLINE_NOTICE, state.notice)
        assertEquals(500L, state.lastUpdatedAt)
    }

    @Test
    fun `офлайн без кэша - Error, а Retry делает принудительное обновление`() = runTest {
        var calls = 0
        people.refreshMainListHandler = {
            calls++
            if (calls == 1) {
                RefreshResult.Failed(IOException())
            } else {
                people.mainList.value = listOf(TestData.alice) // сеть появилась -> Room обновился
                RefreshResult.Updated
            }
        }

        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(UiState.Error("Нет сети или проблема с подключением"), vm.uiState.value.content)

        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf(false, true), people.refreshForceFlags) // старт учитывает TTL, Retry - force
        assertEquals(listOf(1L), vm.ids())
        assertNull(vm.uiState.value.notice)
    }

    @Test
    fun `Flow - последовательность Loading, Error, Content когда фоновая синхронизация наполнила кэш`() = runTest {
        people.refreshMainListHandler = { delay(100); RefreshResult.Failed(IOException()) }
        val vm = createViewModel()

        vm.uiState.map { it.content }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error("Нет сети или проблема с подключением"), awaitItem())

            // WorkManager в фоне записал данные в Room -> экран обновился САМ
            people.mainList.value = listOf(TestData.bob)

            assertEquals(UiState.Content(listOf(PersonRow(TestData.bob, isFavorite = false))), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `статус Скрыть из ленты убирает человека сразу, без запроса в сеть`() = runTest {
        people.mainList.value = listOf(TestData.alice, TestData.bob)
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L), vm.ids())

        notes.setStatus(TestData.bob.id, PersonalStatus.HIDDEN)
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.ids())

        settings.setShowHidden(true)
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L), vm.ids())

        assertEquals("сеть дёргалась только при старте", 1, people.refreshForceFlags.size)
    }

    @Test
    fun `поиск без сети идёт по кэшу и не сохраняется в историю поиска`() = runTest {
        people.searchHandler = { q -> SearchResult(listOf(TestData.bob), fromCache = q == "bo") }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("bo")
        advanceUntilIdle()
        assertEquals(listOf(2L), vm.ids())
        assertEquals(OFFLINE_SEARCH_NOTICE, vm.uiState.value.notice)
        assertTrue(searchHistory.recorded.value.isEmpty())

        vm.onQueryChange("bob")
        advanceUntilIdle()
        assertNull(vm.uiState.value.notice)
        assertEquals(listOf("bob"), vm.uiState.value.recentSearches)
    }

    @Test
    fun `debounce - быстрый ввод даёт один поисковый запрос`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("a")
        advanceTimeBy(100)
        vm.onQueryChange("al")
        advanceTimeBy(100)
        vm.onQueryChange("ali")
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)
        assertTrue(people.searchQueries.isEmpty())

        advanceUntilIdle()
        assertEquals(listOf("ali"), people.searchQueries)
    }

    @Test
    fun `flatMapLatest - устаревший ответ поиска не попадает на экран`() = runTest {
        val slow = CompletableDeferred<List<TvmPerson>>()
        people.searchHandler = { q ->
            if (q == "al") SearchResult(slow.await(), fromCache = false)
            else SearchResult(listOf(TestData.carol), fromCache = false)
        }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("al")
        advanceUntilIdle()
        vm.onQueryChange("car")
        advanceUntilIdle()
        slow.complete(listOf(TestData.alice))
        advanceUntilIdle()

        assertEquals(listOf(TestData.carol.id), vm.ids())
    }
}
