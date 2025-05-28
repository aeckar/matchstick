import java.net.URI

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

dokka {
    moduleName = rootProject.name
    dokkaPublications.html {
        moduleVersion = version.toString()
        outputDirectory = file("docs")
        suppressInheritedMembers = false
        suppressObviousFunctions = true
        failOnWarning = true
    }
    dokkaSourceSets.main {
        includes.from("module.md")
        skipEmptyPackages = true
        sourceLink {
            val sourcePath = "src/main/kotlin"
            localDirectory = file(sourcePath)
            remoteUrl = URI("https://github.com/aeckar/matchstick/tree/$version/lib/$sourcePath")
            remoteLineSuffix = "#L"
        }
    }
    pluginsConfiguration.html {
        separateInheritedMembers = true
        customStyleSheets = files("no-platform.css")
        footerMessage = "© Angel Eckardt 2025"
    }
}