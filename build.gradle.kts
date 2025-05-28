plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "io.github.aeckar"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sf.trove4j:core:3.1.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    explicitApi()
    jvmToolchain(23)
}