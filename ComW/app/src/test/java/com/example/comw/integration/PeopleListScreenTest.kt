package com.example.comw.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeTvmRepository
import com.example.comw.testutil.TestData
import com.example.comw.ui.screen.PeopleListScreen
import com.example.comw.ui.viewmodel.PeopleListViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Интеграционные UI-тесты: настоящий Compose-экран + настоящая ViewModel,
 * но вместо сети — фейковый репозиторий.
 */
@RunWith(AndroidJUnit4::class)
class PeopleListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `ошибка - нажатие Retry - на экране список`() {
        val repo = FakeTvmRepository().apply {
            enqueueListError(IOException())
            enqueueList(listOf(TestData.alice, TestData.bob))
        }
        val vm = PeopleListViewModel(repo, FakeFavoritesRepository())

        composeRule.setContent {
            PeopleListScreen(onOpenDetail = {}, onOpenHistory = {}, viewModel = vm)
        }

        composeRule.onNodeWithText("Нет сети или проблема с подключением").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
        assertEquals(2, repo.listCalls)
    }

    @Test
    fun `клик по элементу открывает детали именно этого id`() {
        val repo = FakeTvmRepository().apply { enqueueList(listOf(TestData.alice, TestData.bob)) }
        val vm = PeopleListViewModel(repo, FakeFavoritesRepository())
        var openedId: Long? = null

        composeRule.setContent {
            PeopleListScreen(onOpenDetail = { openedId = it }, onOpenHistory = {}, viewModel = vm)
        }

        composeRule.onNodeWithText("Bob").performClick()

        assertEquals(TestData.bob.id, openedId)
    }
}
