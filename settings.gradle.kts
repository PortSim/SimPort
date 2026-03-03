plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "SimPort"

include("api")

include("demos")

include("simulator")

include("visuals")

include("ports")
