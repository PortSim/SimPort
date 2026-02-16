plugins {
    id("convention")
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
}

dependencies {
    api(project(":ports"))
    implementation(project(":visuals"))
    implementation(project(":demos"))
}
