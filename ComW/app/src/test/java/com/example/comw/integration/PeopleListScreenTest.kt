package com.example.comw.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.comw.model.RefreshResult
import com.example.comw.testutil.FakeFavoritesRepository
import com.example.comw.testutil.FakeNotesRepository
import com.example.comw.testutil.FakePeopleRepository
import com.example.comw.testutil.FakeSearchHistoryRepository
import com.example.comw.testutil.FakeSettingsRepository
import com.example.comw.testutil.TestData
import com.example.comw.ui.screen.PeopleListScreen
import com.example.comw.ui.viewmodel.PeopleListViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** UI-тесты главного экрана: Compose + настоящая ViewModel + фейковые репозитории. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp") // размер экрана обычного телефона
class PeopleListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val people = FakePeopleRepository()

    private fun viewModel() = PeopleListViewModel(
        people, FakeFavoritesRepository(), FakeNotesRepository(),
        FakeSettingsRepository(), FakeSearchHistoryRepository()
    )

    @Test
    fun `без сети, но с кэшем - видны сохранённые люди и плашка офлайн`() {
        people.mainList.value = listOf(TestData.alice, TestData.bob)
        people.refreshMainListHandler = { RefreshResult.Failed(IOException()) }
        val vm = viewModel()

        composeRule.setContent { PeopleListScreen(onOpenDetail = {}, onOpenHistory = {}, viewModel = vm) }

        composeRule.onNodeWithTag("offline_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
    }

    @Test
    fun `ошибка без кэша - нажатие Retry - на экране список`() {
        var calls = 0
        people.refreshMainListHandler = {
            calls++
            if (calls == 1) RefreshResult.Failed(IOException())
            else {
                people.mainList.value = listOf(TestData.alice, TestData.bob)
                RefreshResult.Updated
            }
        }
        val vm = viewModel()

        composeRule.setContent { PeopleListScreen(onOpenDetail = {}, onOpenHistory = {}, viewModel = vm) }

        composeRule.onNodeWithText("Нет сети или проблема с подключением").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
        assertEquals(2, calls)
    }

    @Test
    fun `клик по элементу открывает детали именно этого id`() {
        people.mainList.value = listOf(TestData.alice, TestData.bob)
        val vm = viewModel()
        var openedId: Long? = null

        composeRule.setContent {
            PeopleListScreen(onOpenDetail = { openedId = it }, onOpenHistory = {}, viewModel = vm)
        }
        composeRule.onNodeWithText("Bob").performClick()

        assertEquals(TestData.bob.id, openedId)
    }
}
