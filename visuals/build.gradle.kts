plugins {
    id("convention")
    id("org.jetbrains.compose") version "1.10.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0"
}

configurations.compileClasspath { exclude(group = "org.jetbrains.compose.material", module = "material") }

dependencies {
    implementation(project(":demos"))
    implementation(project(":stats"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    /* Stuff for the ELK layout algorithm */
    implementation("org.eclipse.xtext:org.eclipse.xtext.xbase.lib:2.35.0")
    implementation("org.eclipse.elk:org.eclipse.elk.core:0.11.0")
    implementation("org.eclipse.elk:org.eclipse.elk.graph:0.11.0")
    implementation("org.eclipse.elk:org.eclipse.elk.alg.layered:0.11.0")
    implementation("org.eclipse.elk:org.eclipse.elk.alg.mrtree:0.11.0")
    implementation("org.eclipse.elk:org.eclipse.elk.alg.force:0.11.0")
    /* Metric visualisations */
    implementation("io.github.ehsannarmani:compose-charts:0.1.1")
}

compose.desktop { application { mainClass = "MainKt" } }
