package com.example.comw.unit

import com.example.comw.model.TvmPerson
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeTvmRepository
import com.example.comw.testutil.MainDispatcherRule
import com.example.comw.testutil.TestData
import com.example.comw.ui.viewmodel.GenderFilter
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.PeopleListViewModel.Companion.SEARCH_DEBOUNCE_MS
import com.example.comw.ui.viewmodel.PersonRow
import com.example.comw.ui.viewmodel.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** ДЗ 6: тесты реактивной логики главного экрана (композиция нескольких потоков). */
@OptIn(ExperimentalCoroutinesApi::class)
class ReactiveSearchTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = FakeTvmRepository().apply {
        enqueueList(listOf(TestData.alice, TestData.bob))
        enqueueList(listOf(TestData.alice, TestData.bob)) // на случай повторной загрузки
    }
    private val favorites = FakeFavoritesRepository()

    private fun createViewModel() = PeopleListViewModel(repo, favorites)

    @Suppress("UNCHECKED_CAST")
    private fun PeopleListViewModel.rows(): List<PersonRow> =
        (uiState.value.content as UiState.Content<List<PersonRow>>).data

    @Test
    fun `debounce - быстрый ввод даёт один запрос с последним текстом`() = runTest {
        repo.searchHandler = { listOf(TestData.bob) }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("b")
        advanceTimeBy(100)
        vm.onQueryChange("bo")
        advanceTimeBy(100)
        vm.onQueryChange("bob")
        advanceTimeBy(SEARCH_DEBOUNCE_MS - 1)

        assertTrue("пока пользователь печатает, запросов нет", repo.searchQueries.isEmpty())

        advanceUntilIdle()
        assertEquals(listOf("bob"), repo.searchQueries)
        assertEquals(listOf(TestData.bob.id), vm.rows().map { it.person.id })
    }

    @Test
    fun `distinctUntilChanged - тот же запрос с пробелами не уходит в сеть повторно`() = runTest {
        repo.searchHandler = { listOf(TestData.bob) }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("bob")
        advanceUntilIdle()
        vm.onQueryChange("bob  ")
        advanceUntilIdle()

        assertEquals(listOf("bob"), repo.searchQueries)
    }

    @Test
    fun `flatMapLatest - ответ устаревшего поиска не попадает на экран`() = runTest {
        val slow = CompletableDeferred<List<TvmPerson>>()
        repo.searchHandler = { q -> if (q == "al") slow.await() else listOf(TestData.carol) }
        val vm = createViewModel()

        val seen = mutableListOf<UiState<List<PersonRow>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.map { it.content }.toList(seen)
        }
        advanceUntilIdle()

        vm.onQueryChange("al")
        advanceUntilIdle()                 // поиск "al" висит
        vm.onQueryChange("car")
        advanceUntilIdle()                 // поиск "car" ответил
        slow.complete(listOf(TestData.alice))
        advanceUntilIdle()                 // "al" ответил слишком поздно

        assertEquals(listOf(TestData.carol.id), vm.rows().map { it.person.id })
        val aliceShownAfterSearch = seen.drop(1).any { state ->
            state is UiState.Content && state.data.any { it.person.id == TestData.alice.id } &&
                state.data.none { it.person.id == TestData.bob.id }
        }
        assertTrue("результат запроса al не должен был отобразиться", !aliceShownAfterSearch)
    }

    @Test
    fun `смена фильтра пересчитывает список без нового запроса в сеть`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        assertEquals(1, repo.listCalls)

        vm.onFilterChange(GenderFilter.FEMALE)
        advanceUntilIdle()
        assertEquals(listOf(TestData.alice.id), vm.rows().map { it.person.id })

        vm.onFilterChange(GenderFilter.MALE)
        advanceUntilIdle()
        assertEquals(listOf(TestData.bob.id), vm.rows().map { it.person.id })

        assertEquals("фильтр работает локально", 1, repo.listCalls)
    }

    @Test
    fun `изменение избранного в data-слое само обновляет звёздочки без перезагрузки`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        favorites.add(TestData.bob) // как будто добавили с экрана деталей
        advanceUntilIdle()

        assertEquals(listOf(false, true), vm.rows().map { it.isFavorite })
        assertEquals(listOf(TestData.bob.id), vm.uiState.value.favorites.map { it.person.id })
        assertEquals(1, repo.listCalls)
    }

    @Test
    fun `пустой результат поиска - Empty, очистка строки возвращает обычный список`() = runTest {
        repo.searchHandler = { emptyList() }
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("zzz")
        advanceUntilIdle()
        assertEquals(UiState.Empty, vm.uiState.value.content)
        assertEquals("zzz", vm.uiState.value.query)

        vm.onQueryChange("")
        advanceUntilIdle()
        assertEquals(2, repo.listCalls)
        assertEquals(2, vm.rows().size)
    }
}
