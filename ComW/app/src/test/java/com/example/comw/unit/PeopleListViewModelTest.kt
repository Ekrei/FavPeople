package com.example.comw.unit

import app.cash.turbine.test
import com.example.comw.model.TvmPerson
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeTvmRepository
import com.example.comw.testutil.MainDispatcherRule
import com.example.comw.testutil.TestData
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    @Test
    fun `начальное состояние - Loading`() = runTest {
        repo.enqueueList(listOf(TestData.alice))

        val vm = createViewModel()

        // Корутина загрузки ещё не выполнялась (StandardTestDispatcher),
        // поэтому экран должен быть в состоянии Loading.
        assertEquals(UiState.Loading, vm.uiState.value)
    }

    @Test
    fun `успешная загрузка - Content со списком`() = runTest {
        repo.enqueueList(listOf(TestData.alice, TestData.bob))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(UiState.Content(listOf(TestData.alice, TestData.bob)), vm.uiState.value)
    }

    @Test
    fun `ошибка сети - Error с понятным сообщением`() = runTest {
        repo.enqueueListError(IOException("timeout"))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(UiState.Error("Нет сети или проблема с подключением"), vm.uiState.value)
    }

    @Test
    fun `пустой ответ - именно Empty, а не Content с пустым списком`() = runTest {
        repo.enqueueList(emptyList())

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(UiState.Empty, vm.uiState.value)
    }

    @Test
    fun `на экран попадают только первые 20 элементов`() = runTest {
        repo.enqueueList((1L..30L).map { TestData.person(it) })

        val vm = createViewModel()
        advanceUntilIdle()

        val content = vm.uiState.value as UiState.Content<List<TvmPerson>>
        assertEquals(20, content.data.size)
        assertEquals(1L, content.data.first().id)
    }

    @Test
    fun `retry после ошибки действительно делает новый запрос и показывает данные`() = runTest {
        repo.enqueueListError(IOException())
        repo.enqueueList(listOf(TestData.alice))

        val vm = createViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is UiState.Error)
        assertEquals(1, repo.listCalls)

        vm.load() // кнопка Retry
        advanceUntilIdle()

        assertEquals(2, repo.listCalls) // был второй поход в "сеть"
        assertEquals(UiState.Content(listOf(TestData.alice)), vm.uiState.value)
    }

    @Test
    fun `Flow - полная последовательность Loading, Error, Loading, Content`() = runTest {
        // имитируем сеть с задержкой, чтобы Loading был виден как отдельное состояние
        repo.enqueueListAnswer { delay(100); throw IOException() }
        repo.enqueueListAnswer { delay(100); listOf(TestData.bob) }

        val vm = createViewModel()

        vm.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Error("Нет сети или проблема с подключением"), awaitItem())

            vm.load()

            assertEquals(UiState.Loading, awaitItem())
            assertEquals(UiState.Content(listOf(TestData.bob)), awaitItem())
            expectNoEvents() // лишних (дублирующихся) эмиссий нет
        }
    }

    @Test
    fun `Flow - повторная загрузка отменяет старый запрос, и его результат не попадает в UI`() = runTest {
        val slowFirstResponse = CompletableDeferred<List<TvmPerson>>()
        repo.enqueueListAnswer { slowFirstResponse.await() } // первый запрос "висит"
        repo.enqueueList(listOf(TestData.bob))                // второй отвечает сразу

        val vm = createViewModel()

        vm.uiState.test {
            assertEquals(UiState.Loading, awaitItem())
            runCurrent() // первый запрос стартовал и ждёт ответа

            vm.load()    // пользователь нажал "обновить" ещё раз
            assertEquals(UiState.Content(listOf(TestData.bob)), awaitItem())

            // теперь "приходит" ответ первого (устаревшего) запроса
            slowFirstResponse.complete(listOf(TestData.alice))
            runCurrent()

            expectNoEvents() // Alice так и не появилась на экране
        }
        assertEquals(UiState.Content(listOf(TestData.bob)), vm.uiState.value)
        assertEquals(2, repo.listCalls)
    }
}
