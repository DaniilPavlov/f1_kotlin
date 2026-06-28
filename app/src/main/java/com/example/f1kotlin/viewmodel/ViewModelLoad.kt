package com.example.f1kotlin.viewmodel

import com.example.f1kotlin.domain.AppException
import com.example.f1kotlin.domain.AsyncValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Общий паттерн загрузки для всех ViewModel:
 * - отмена предыдущего [Job] при повторном вызове;
 * - не затирать уже показанные данные (Room/peek) ошибкой сети.
 */
class LoadJobHolder {
    private var job: Job? = null

    fun launch(scope: CoroutineScope, block: suspend CoroutineScope.() -> Unit) {
        job?.cancel()
        job = scope.launch(block = block)
    }
}

/**
 * Применяет [Result]: успех всегда обновляет UI; ошибку — только если кэша ещё не было.
 *
 * @param hasCachedValue true, если peek/Room уже отдал [AsyncValue.Value] на экран.
 */
inline fun <T> Result<T>.applyUnlessCached(
    hasCachedValue: Boolean,
    crossinline onSuccess: (T) -> Unit,
    crossinline onFailure: (AppException) -> Unit,
) {
    onSuccess { value -> onSuccess(value) }
    onFailure { e ->
        if (!hasCachedValue) {
            onFailure(e as AppException)
        }
    }
}

/** Перегрузка: «есть кэш» = текущее состояние уже [AsyncValue.Value]. */
inline fun <T> Result<T>.applyUnlessCached(
    current: AsyncValue<*>,
    crossinline onSuccess: (T) -> Unit,
    crossinline onFailure: (AppException) -> Unit,
) = applyUnlessCached(
    hasCachedValue = current is AsyncValue.Value,
    onSuccess = onSuccess,
    onFailure = onFailure,
)
