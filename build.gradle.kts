plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
}

val minecraftVersion = properties["minecraft_version"]!! as String
val parchmentVersion = properties["parchment_version"]!! as String
val loaderVersion = properties["loader_version"]!! as String

val modVersion = properties["mod_version"]!! as String
val supportedVersions = properties["supported_versions"]!! as String
val archivesBaseName = properties["archives_base_name"]!! as String

val fabricVersion = properties["fabric_version"]!! as String
val packConverterVersion = properties["pack_converter_version"]!! as String

val targetJavaVersion = 21

repositories {
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }

    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }

    maven {
        name = "Open Collaboration"
        url = uri("https://repo.opencollab.dev/main")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${parchmentVersion}@zip")
    })

    modImplementation("net.fabricmc:fabric-loader:${loaderVersion}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${fabricVersion}")

    include(implementation("com.github.GeyserMC.unnamed-creative:creative-api:817fa982c4")!!)
    include(implementation("com.github.GeyserMC.unnamed-creative:creative-serializer-minecraft:817fa982c4")!!)
    include(implementation("org.geysermc.pack:converter:${packConverterVersion}")!!)
}

tasks {
    processResources {
        inputs.property("version", modVersion)
        inputs.property("supported_versions", supportedVersions)
        inputs.property("loader_version", loaderVersion)
        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to modVersion,
                    "supported_versions" to supportedVersions,
                    "loader_version" to loaderVersion
                )
            )
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${archivesBaseName}" }
        }
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = targetJavaVersion
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    withSourcesJar()
}

loom {
    runs {
        named("server") {
            runDir = "run-server"
        }
    }
}
