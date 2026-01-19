plugins {
    id("convention")
    id("org.jetbrains.compose") version "1.10.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
}

configurations.compileClasspath { exclude(group = "org.jetbrains.compose.material") }

dependencies {
    implementation(project(":demos"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("io.github.justdeko:kuiver:0.2.1")
}

compose.desktop { application { mainClass = "MainKt" } }
