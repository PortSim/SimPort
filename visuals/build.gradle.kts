plugins {
    kotlin("jvm")
}

group = "com.group7"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":ports"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}