package io.github.aeckar.parsing

import java.io.Serial

/** When thrown, signals that -1 should be returned from [collect][MatcherImpl.collectMatches]. */
internal data object Failure : Throwable() {
    @Serial
    private fun readResolve(): Any = Failure

    inline fun handle(block: () -> Int) = try {
        block()
    } catch (_: Failure) {
        -1
    }
}