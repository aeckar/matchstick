module matchstick {
    requires transitive kotlin.stdlib;

    exports io.github.aeckar.parsing.dsl;
    exports io.github.aeckar.parsing.patterns;
    exports io.github.aeckar.parsing;
    exports io.github.aeckar.parsing.state;
}