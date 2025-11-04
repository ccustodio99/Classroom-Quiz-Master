package com.example.lms.core.common

import com.example.lms.core.model.LmsResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> runCatchingResult(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> T,
): LmsResult<T> = try {
    val value = withContext(dispatcher) { block() }
    LmsResult.Success(value)
} catch (t: Throwable) {
    LmsResult.Error(t)
}

inline fun <T, R> LmsResult<T>.map(transform: (T) -> R): LmsResult<R> = when (this) {
    is LmsResult.Success -> LmsResult.Success(transform(value))
    is LmsResult.Error -> this
}

inline fun <T> LmsResult<T>.onSuccess(action: (T) -> Unit): LmsResult<T> = apply {
    if (this is LmsResult.Success) action(value)
}

inline fun <T> LmsResult<T>.onError(action: (Throwable) -> Unit): LmsResult<T> = apply {
    if (this is LmsResult.Error) action(throwable)
}

