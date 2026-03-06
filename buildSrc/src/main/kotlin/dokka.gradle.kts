plugins { id("org.jetbrains.dokka") }

dokka {
    dokkaSourceSets.configureEach {
        skipDeprecated = true // Skips deprecated API
        reportUndocumented = true // Reports if any public API are undocumented

        jdkVersion = 21
        moduleName = project.name
    }
}
