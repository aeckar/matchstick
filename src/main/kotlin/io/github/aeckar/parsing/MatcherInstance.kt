package io.github.aeckar.parsing

internal abstract class MatcherInstance() : RichMatcher {
    override val identity: RichMatcher get() = this

    override fun hashCode() = id.hashCode()
    override fun coreIdentity(): RichMatcher = this

    override fun equals(other: Any?): Boolean {
        return this === other || other is RichMatcher && other.coreLogic() === coreLogic()
    }
}