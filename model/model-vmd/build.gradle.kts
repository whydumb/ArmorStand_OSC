plugins {
    alias(libs.plugins.kotlin)
}

version = rootProject.version
group = "top.fifthlight.armorstand"

dependencies {
    implementation(project(":model:model-base"))
    testImplementation(kotlin("test"))
}

val testFile = file("src/test/resources/animation.vmd")
if (!testFile.isFile) {
    tasks.test {
        enabled = false
    }
}
