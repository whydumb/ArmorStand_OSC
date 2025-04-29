import java.io.FileNotFoundException
import java.util.Properties

plugins {
    alias(libs.plugins.fabric.loom) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

version = "0.0.1+dev"

project.ext["localProperties"] = Properties().apply {
    try {
        rootProject.file("local.properties").reader().use(::load)
    } catch (_: FileNotFoundException) {
    }
}

subprojects {
    repositories {
        mavenCentral()
        maven {
            name = "Terraformers"
            url = uri("https://maven.terraformersmc.com/")
        }
        maven {
            name = "Wisp Forest"
            url = uri("https://maven.wispforest.io/releases/")
        }
    }
}
