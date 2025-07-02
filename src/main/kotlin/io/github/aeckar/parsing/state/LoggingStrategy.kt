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
    /** Returns this text style printing ANSI escape codes if [supportsAnsi] is true. */
    public fun TextStyle.ifSupported(): TextStyle = this given supportsAnsi
}