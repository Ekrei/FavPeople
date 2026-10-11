package com.example.comw.unit

import app.cash.turbine.test
import com.example.comw.model.TvmPerson
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeTvmRepository
import com.example.comw.testutil.MainDispatcherRule
import com.example.comw.testutil.TestData
import com.example.comw.ui.viewmodel.PeopleListUiState
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.PersonRow
import com.example.comw.ui.viewmodel.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleListViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = FakeTvmRepository()
    private val favorites = FakeFavoritesRepository()

    private fun createViewModel() = PeopleListViewModel(repo, favorites)

    private fun rows(vararg people: TvmPerson) = people.map { PersonRow(it, isFavorite = false) }

    @Test
    fun `начальное состояние - Loading с пустым запросом и фильтром Все`() = runTest {
        repo.enqueueList(listOf(TestData.alice))

        val vm = createViewModel()

        assertEquals(PeopleListUiState(), vm.uiState.value)
        assertEquals(UiState.Loading, vm.uiState.value.content)
    }

    @Test
    fun `успешная загрузка - Content со списком`() = runTest {
        repo.enqueueList(listOf(TestData.alice, TestData.bob))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(UiState.Content(rows(TestData.alice, TestData.bob)), vm.uiState.value.content)
    }

    @Test
    fun `ошибка сети - Error с понятным сообщением`() = runTest {
        repo.enqueueListError(IOException("timeout"))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(UiState.Error("Нет сети или проблема с подключением"), vm.uiState.value.content)
    }

    @Test
    fun `пустой ответ - именно Empty, а не Content с пустым списком`() = runTest {
        repo.enqueueList(emptyList())

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(UiState.Empty, vm.uiState.value.content)
    }

    @Test
    fun `на экран попадают только первые 20 элементов`() = runTest {
        repo.enqueueList((1L..30L).map { TestData.person(it) })

        val vm = createViewModel()
        advanceUntilIdle()

        @Suppress("UNCHECKED_CAST")
        val content = vm.uiState.value.content as UiState.Content<List<PersonRow>>
        assertEquals(20, content.data.size)
    }

    @Test
    fun `retry после ошибки действительно делает новый запрос и показывает данные`() = runTest {
        repo.enqueueListError(IOException())
        repo.enqueueList(listOf(TestData.alice))

        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.content is UiState.Error)

        vm.refresh() // кнопка Retry -> событие в SharedFlow
        advanceUntilIdle()

        assertEquals(2, repo.listCalls)
        assertEquals(UiState.Content(rows(TestData.alice)), vm.uiState.value.content)
    }

    @Test
    fun `Flow - полная последовательность Loading, Error, Loading, Content`() = runTest {
        repo.enqueueListAnswer { delay(100); throw IOException() }
        repo.enqueueListAnswer { delay(100); listOf(TestData.bob) }

        val vm = createViewModel()

        vm.uiState.map { it.content }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error("Нет сети или проблема с подключением"), awaitItem())

            vm.refresh()

            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Content(rows(TestData.bob)), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `Flow - повторное обновление отменяет старый запрос, и его результат не попадает в UI`() = runTest {
        val slowFirstResponse = CompletableDeferred<List<TvmPerson>>()
        repo.enqueueListAnswer { slowFirstResponse.await() }
        repo.enqueueList(listOf(TestData.bob))

        val vm = createViewModel()

        vm.uiState.map { it.content }.distinctUntilChanged().test {
            assertEquals(UiState.Loading, awaitItem())
            runCurrent() // первый запрос "висит"

            vm.refresh()
            assertEquals(UiState.Content(rows(TestData.bob)), awaitItem())

            slowFirstResponse.complete(listOf(TestData.alice))
            runCurrent()
            expectNoEvents()
        }
        assertEquals(2, repo.listCalls)
    }
}
