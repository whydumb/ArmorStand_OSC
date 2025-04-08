import java.util.Properties

plugins {
	alias(libs.plugins.fabric.loom)
	alias(libs.plugins.kotlin)
	alias(libs.plugins.kotlin.serialization)
}

version = rootProject.version
group = "top.fifthlight.armorstand"

val localProperties: Properties by rootProject.ext

base {
	archivesName = "armorstand"
}

loom {
	accessWidenerPath = file("src/main/resources/armorstand.accesswidener")
	mixin {
		useLegacyMixinAp = false
	}
	localProperties["minecraft.vm-args"]?.toString()?.split(":")?.let { jvmArgs ->
		runs.configureEach {
			vmArgs(jvmArgs)
			vmArg("-Darmorstand.debug=true")
		}
	}
}

dependencies {
	minecraft(libs.minecraft)
	mappings(variantOf(libs.yarn) { classifier("v2") })
	modImplementation(libs.fabric.loader)

	modImplementation(libs.modmenu)

	modImplementation(libs.fabric.api)
	modImplementation(libs.fabric.language.kotlin)

	implementation(project(":model:model-base"))
	implementation(project(":model:model-gltf"))
	implementation(project(":model:model-pmx"))
	implementation(project(":model:model-pmd"))
	include(project(":model:model-base"))
	include(project(":model:model-gltf"))
	include(project(":model:model-pmx"))
	include(project(":model:model-pmd"))
}

tasks.processResources {
	inputs.properties("version" to project.version)

	filesMatching("fabric.mod.json") {
		expand("version" to inputs.properties["version"])
	}
}

java {
	withSourcesJar()

	toolchain {
		sourceCompatibility = JavaVersion.VERSION_21
		targetCompatibility = JavaVersion.VERSION_21
	}
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_armorstand"}
	}
}
