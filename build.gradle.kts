plugins {
    id("convention")
    id("org.jetbrains.dokka")
}

dependencies { api(project(":simulator")) }

dependencies {
    dokka(project(":ports"))
    dokka(project(":simulator"))
    dokka(project(":api"))
}
