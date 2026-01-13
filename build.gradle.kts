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

    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.test {
    useJUnitPlatform()
}
