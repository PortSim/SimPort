plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
    implementation("io.kotest:io.kotest.gradle.plugin:6.0.7")
    implementation("org.jetbrains.kotlin.plugin.power-assert:org.jetbrains.kotlin.plugin.power-assert.gradle.plugin:2.3.0")
    implementation("com.ncorti.ktfmt.gradle:plugin:0.25.0")
}