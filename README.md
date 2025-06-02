# Matchstick

# Style Guide
- Use Android style guide for all other concerns
- Constants and helper functions should be top-level
- Keep mutable state for one-time functions inside nested classes
- Use collections with boxed primitives unless doing so incurs a huge performance impact
- Group single-line functions before all others so that they can be referenced more easily
- Minimize exposure of internal API
- Use `also` instead of buffer value
- chained calls to multiline scope {} should be connected to bracket
```kotlin
myScope {   // Okay
    
}.also { }


myScope {   // WRONG
    
}
    .also { }
```