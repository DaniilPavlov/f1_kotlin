package com.example.f1_kotlin.domain

/**
 * Универсальная обёртка для асинхронных данных в UI.
 *
 * GoF Behavioral State — поведение UI/ViewModel зависит от текущего варианта
 * ([Loading] / [Value] / [Error]); экран через `when` вместо разрозненных флагов.
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
