package com.classroom.quizmaster.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Stable replacement for [kotlinx.coroutines.flow.flatMapLatest] that avoids Experimental API usage.
 */
fun <T, R> Flow<T>.switchMapLatest(transform: suspend (T) -> Flow<R>): Flow<R> = channelFlow {
    var currentJob: Job? = null
    collect { upstream ->
        currentJob?.cancel()
        currentJob = launch {
            transform(upstream).collect { send(it) }
        }
    }
    awaitClose { currentJob?.cancel() }
}

