package com.softbankrobotics.pddlplanning.utils

import android.util.Log
import kotlinx.coroutines.*

/**
 * A coroutine exception handler that forwards the issue to the logs.
 */
fun createLoggingCouroutineExceptionHandler(tag: String): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, throwable ->
        Log.w(tag, "Uncaught exception", throwable)
    }
}

/**
 * Creates a couroutine scope configured to not favor specific threads,
 * and is therefore not recommended for blocking calls.
 * Exceptions are automatically caught.
 */
fun createAsyncCoroutineScope(tag: String): CoroutineScope {
    return CoroutineScope(Dispatchers.IO + SupervisorJob() + createLoggingCouroutineExceptionHandler(tag))
}

/**
 * Creates a couroutine scope configured to not favor specific threads,
 * and is therefore not recommended for blocking calls.
 * Exceptions are automatically caught.
 * The tag is automatically computed from the class name of the method enclosing the caller.
 */
fun createAsyncCoroutineScope(): CoroutineScope {
    val callerClassName = Thread.currentThread().stackTrace[1].className.substringAfterLast('.')
    return createAsyncCoroutineScope(callerClassName)
}
