plugins {
    kotlin("jvm") version "2.2.21"
}

group = "com.group7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")

    // Mocking
    testImplementation("io.mockk:mockk:1.13.10")

    // Coroutines (if needed)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}