plugins { id("convention") }

dependencies { api(project(":simulator")) }

dependencies {
    dokka(project(":ports"))
    dokka(project(":simulator"))
    dokka(project(":visuals"))
    dokka(project(":api"))
}
