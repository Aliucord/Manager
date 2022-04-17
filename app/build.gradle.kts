val pagingVersion = "3.1.0"
val accompanistVersion = "0.23.0"
val composeVersion = "1.1.1"
val ktorVersion = "2.0.0"

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
    kotlin("plugin.serialization") version "1.6.10"
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.aliucord.manager"
        minSdk = 24
        targetSdk = 31
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "TAG", "\"AliucordManager\"")
        buildConfigField("String", "SUPPORT_SERVER", "\"EsNDvBaHVU\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }

    buildFeatures.compose = true
    composeOptions.kotlinCompilerExtensionVersion = composeVersion
}

kotlin {
    sourceSets {
        debug {
            kotlin.srcDir("build/generated/ksp/debug/kotlin")
        }
        release {
            kotlin.srcDir("build/generated/ksp/release/kotlin")
        }
    }
}

dependencies {
    // core dependencies
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.0-beta02")

    // compose dependencies
    implementation("androidx.compose.ui:ui:${composeVersion}")
    implementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    implementation("androidx.compose.material:material:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.0.0-alpha09")
    implementation("androidx.paging:paging-compose:1.0.0-alpha14")

    // accompanist dependencies
    implementation("com.google.accompanist:accompanist-systemuicontroller:${accompanistVersion}")
    implementation("com.google.accompanist:accompanist-permissions:${accompanistVersion}")

    // compose destinations
    implementation("io.github.raamcosta.compose-destinations:core:1.4.2-beta")
    ksp("io.github.raamcosta.compose-destinations:ksp:1.4.2-beta")

    // other dependencies
    implementation("io.coil-kt:coil-compose:1.4.0")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("de.upb.cs.swt:axml:2.1.1") // 2.1.2 is broken btw
    implementation("com.android.tools.build:apksig:7.1.3")
    implementation("com.github.diamondminer88:zip-android:1.0.0@aar")

    // ktor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}
