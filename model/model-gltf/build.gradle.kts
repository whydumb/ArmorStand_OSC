plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.serialization)
}

version = rootProject.version
group = "top.fifthlight.armorstand"

dependencies {
    implementation(project(":model:model-base"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}
