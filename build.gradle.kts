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
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
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