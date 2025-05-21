@file:Suppress("UnstableApiUsage")

import java.util.*

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

	splitEnvironmentSourceSets()

	mixin {
		useLegacyMixinAp = false
	}

	mods {
		create("armorstand") {
			sourceSet("main")
			sourceSet("client")
		}
	}

	runs {
		getByName("client") {
			runDir("run/client")
			programArg("--tracy")
		}
		getByName("server") {
			runDir("run/server")
		}
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

	modImplementation(libs.owo.lib)

	listOf(
		":model:model-base",
		":model:model-gltf",
		":model:model-pmx",
		":model:model-pmd",
		":model:model-vmd",
	).forEach { name ->
		implementation(project(name))
		include(project(name))
	}
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
