plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
    id("io.kotest")
    id("com.ncorti.ktfmt.gradle")
}

group = "com.group7"

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

val kotestVersion = "6.0.7"

dependencies {
    testImplementation(kotlin("test"))

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")

    // Mocking
    testImplementation("io.mockk:mockk:1.13.10")

    // Coroutines (if needed)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Kotest
    testImplementation("io.kotest:kotest-framework-engine:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
}

kotlin {
    jvmToolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xopt-in=kotlin.contracts.ExperimentalContracts")
    }
}

tasks.test { useJUnitPlatform() }

powerAssert { functions = listOf("io.kotest.matchers.shouldBe") }

ktfmt {
    kotlinLangStyle()
    maxWidth = 120
}
