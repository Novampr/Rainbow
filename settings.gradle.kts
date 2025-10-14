pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
    includeBuild("build-logic")
}

include(":rainbow")
include(":client")
include(":datagen")

rootProject.name = "rainbow-parent"
