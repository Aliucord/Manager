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
        maven { // TODO: remove when Ktor 3.2.1 released
            name = "ktor-eap"
            url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        }
    }
}

rootProject.name = "AliucordManager"
include(":app")
