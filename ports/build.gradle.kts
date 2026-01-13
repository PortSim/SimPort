plugins {
    kotlin("jvm")
}

group = "com.group7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":simulator"))
    implementation("ca.umontreal.iro.simul:ssj:3.3.2")

    // Testing
    testImplementation(kotlin("test"))

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")

    // Mocking
    testImplementation("io.mockk:mockk:1.13.10")

    // Kotest
    testImplementation("io.kotest:kotest-runner-junit5:6.0.7")
    testImplementation("io.kotest:kotest-framework-engine:6.0.7")
    testImplementation("io.kotest:kotest-assertions-core:6.0.7")
}

tasks.test {
    useJUnitPlatform()
}