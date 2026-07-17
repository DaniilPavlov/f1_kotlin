package com.example.f1_kotlin.domain

/**
 * Универсальная обёртка для асинхронных данных в UI.
 *
 * Вместо отдельных флагов `isLoading`, `error`, `data` ViewModel хранит одно поле
 * типа [AsyncValue], и экран через `when` решает, что показать: спиннер, ошибку или контент.
 *
 * Это похоже на sealed class / union type: компилятор заставляет обработать все варианты.
 *
 * @param T тип успешных данных (список гонщиков, гонка и т.д.)
 */
sealed class AsyncValue<out T> {

    /** Данные ещё грузятся — показываем [LoadingIndicator]. */
    data object Loading : AsyncValue<Nothing>()

    /** Успешный ответ с данными. */
    data class Value<T>(val value: T) : AsyncValue<T>()

    /** Ошибка с заголовком и опциональным подзаголовком для [ErrorBody]. */
    data class Error(val message: String, val subtitle: String? = null) : AsyncValue<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isError: Boolean get() = this is Error

    /** Безопасно достаёт значение или возвращает null, если это не [Value]. */
    fun getOrNull(): T? = (this as? Value)?.value
}

/**
     * Доменное исключение с пользовательским текстом на русском.
     *
     * Слой API ([ApiCallHandler.safeCall]) превращает сетевые/парсинг-ошибки в [AppException],
     * чтобы UI не показывал сырой stack trace.
    */
data class AppException(
    val title: String,
    val subtitle: String? = null,
) : Exception(title)
