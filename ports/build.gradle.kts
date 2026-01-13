plugins {
    id("convention")
}

dependencies {
    api(project(":simulator"))
    implementation("ca.umontreal.iro.simul:ssj:3.3.2")
}