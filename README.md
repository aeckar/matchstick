# Matchstick

# Style Guide
- Use Android style guide for all other concerns
- Constants and helper functions should be top-level
- Keep mutable state for one-time functions inside nested classes
- Use collections with boxed primitives unless doing so incurs a huge performance impact
- Group single-line functions before all others so that they can be referenced more easily
- Minimize exposure of internal API