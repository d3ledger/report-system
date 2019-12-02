package com.d3.notification.exception

/**
 * Exception wrapper that is used to signalize that the failed operation may be repeated
 */
class RepeatableError(message: String, cause: Throwable) : Exception(message, cause)
