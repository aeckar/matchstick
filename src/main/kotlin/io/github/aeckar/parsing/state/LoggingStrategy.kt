package io.github.aeckar.parsing.state

import io.github.aeckar.ansi.TextStyle
import io.github.aeckar.ansi.given
import io.github.oshai.kotlinlogging.KLogger

/**
 * Assigns metadata to a logger.
 * @param logger the logger used to print messages according to some configuration
 * @param supportsAnsi if false, logged message should not contain any ANSI escape codes
 */
public class LoggingStrategy @PublishedApi internal constructor(    // Inlined by 'loggerOf' and 'ansi'
    public val logger: KLogger,
    public val supportsAnsi: Boolean
) {
    internal val blue = io.github.aeckar.ansi.blue given supportsAnsi
    internal val green = io.github.aeckar.ansi.green given supportsAnsi
    internal val red = io.github.aeckar.ansi.red given supportsAnsi
    internal val grey = io.github.aeckar.ansi.grey given supportsAnsi

    @PublishedApi   // Inlined by 'parse'
    internal val yellow: TextStyle = io.github.aeckar.ansi.yellow given supportsAnsi
}