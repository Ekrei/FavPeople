package com.example.comw.ui.viewmodel

/**
 * Возможные состояния экрана.
 * sealed = набор вариантов закрыт, и `when` обязан обработать каждый.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
