@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "aliucord"
            url = uri("https://maven.aliucord.com/snapshots")
        }
        maven { // TODO: remove, refer to libs.versions.toml
            name = "jitpack"
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "AliucordManager"
include(":app")
