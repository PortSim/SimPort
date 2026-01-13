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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}