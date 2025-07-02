package io.github.aeckar.parsing.state

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * Returns a logging strategy with the given configuration and whose logger
 * is given by the qualified name of type [T].
 */
public inline fun <reified T : Any> T.classLogger(supportsAnsi: Boolean = true): LoggingStrategy {
    return loggerOf<T>(supportsAnsi)
}

/**
 * Returns a logging strategy with the given configuration and whose logger
 * is given by the qualified name of type [T].
 */
public inline fun <reified T : Any> loggerOf(supportsAnsi: Boolean = true): LoggingStrategy {
    return LoggingStrategy(KotlinLogging.logger(T::class.qualifiedName!!), supportsAnsi)
}

/** Returns a logging strategy with the given configuration and logger. */
public fun KLogger.ansi(supportsAnsi: Boolean = true): LoggingStrategy {
    return LoggingStrategy(this, supportsAnsi)
}