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
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}