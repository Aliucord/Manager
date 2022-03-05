/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

val pagingVersion = "3.1.0"
val accompanistVersion = "0.23.0"
val composeVersion = "1.1.0"
val ktorVersion = "1.6.7"

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "1.6.10-1.0.4"
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
    implementation(fileTree("./libs"))
    // core dependencies
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.core:core-ktx:1.7.0")
//    implementation("com.github.aliucord:libzip:1.0.1")
    implementation("androidx.core:core-splashscreen:1.0.0-beta01")

    // compose dependencies
    implementation("androidx.compose.ui:ui:${composeVersion}")
    implementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    implementation("androidx.compose.material:material:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.0.0-alpha06")
    implementation("androidx.paging:paging-compose:1.0.0-alpha14")

    // accompanist dependencies
    implementation("com.google.accompanist:accompanist-systemuicontroller:${accompanistVersion}")
    implementation("com.google.accompanist:accompanist-permissions:${accompanistVersion}")

    // compose destinations
    implementation("io.github.raamcosta.compose-destinations:core:1.3.2-beta")
    ksp("io.github.raamcosta.compose-destinations:ksp:1.3.2-beta")

    // other dependencies
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("io.coil-kt:coil-compose:1.4.0")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.65")

    // ktor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-gson:$ktorVersion")
}