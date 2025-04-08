pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

include(":mod")
include(":model:model-base")
include(":model:model-gltf")
include(":model:model-pmx")
include(":model:model-pmd")
