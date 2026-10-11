package com.example.comw.unit

import androidx.lifecycle.SavedStateHandle
import com.example.comw.navigation.Routes
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeHistoryRepository
import com.example.comw.testutil.FakeTvmRepository
import com.example.comw.testutil.MainDispatcherRule
import com.example.comw.testutil.TestData
import com.example.comw.ui.viewmodel.PeopleDetailViewModel
import com.example.comw.ui.viewmodel.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repo = FakeTvmRepository()
    private val favorites = FakeFavoritesRepository()
    private val history = FakeHistoryRepository()

    private fun createViewModel(id: Long) = PeopleDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.Detail.ARG_ID to id)),
        repository = repo,
        favoritesRepository = favorites,
        historyRepository = history
    )

    @Test
    fun `id берётся из SavedStateHandle и запрашиваются детали именно этого человека`() = runTest {
        repo.enqueueDetail(TestData.detail(42))

        val vm = createViewModel(id = 42)
        advanceUntilIdle()

        assertEquals(listOf(42L), repo.requestedDetailIds)
        assertEquals(42L, vm.personId)
        assertEquals(UiState.Content(TestData.detail(42)), vm.uiState.value)
    }

    @Test(expected = IllegalStateException::class)
    fun `без id в SavedStateHandle ViewModel не создаётся`() {
        PeopleDetailViewModel(SavedStateHandle(), repo, favorites, history)
    }

    @Test
    fun `успешный просмотр записывается в историю, ошибка - нет`() = runTest {
        repo.enqueueDetailError(RuntimeException("500"))
        repo.enqueueDetail(TestData.detail(7, "Dave"))

        val vm = createViewModel(id = 7)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is UiState.Error)
        assertTrue(history.recorded.value.isEmpty())

        vm.load() // Retry
        advanceUntilIdle()

        assertEquals(listOf(7L), history.recorded.value.map { it.person.id })
    }

    @Test
    fun `toggleFavorite добавляет, а повторный - убирает`() = runTest {
        repo.enqueueDetail(TestData.detail(5))
        val vm = createViewModel(id = 5)
        advanceUntilIdle()

        vm.toggleFavorite()
        advanceUntilIdle()
        assertEquals(listOf(5L), favorites.ids())

        vm.toggleFavorite()
        advanceUntilIdle()
        assertTrue(favorites.ids().isEmpty())
    }
}
