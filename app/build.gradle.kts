/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

val accompanistVersion = "0.14.0"
val composeVersion = "1.0.0-rc02"

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "com.aliucord.manager"
        minSdk = 24
        targetSdk = 29
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }
}

dependencies {
    // core dependencies
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("com.google.android.material:material:1.4.0")

    // compose dependencies
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.compose.ui:ui:${composeVersion}")
    implementation("androidx.compose.material:material:${composeVersion}")
    implementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0-rc02")
    implementation("androidx.navigation:navigation-compose:2.4.0-rc01")

    // accompanist dependencies
    implementation("com.google.accompanist:accompanist-systemuicontroller:${accompanistVersion}")
    implementation("com.google.accompanist:accompanist-permissions:${accompanistVersion}")

    // other dependencies
    implementation("com.google.code.gson:gson:2.8.7")
    implementation("io.coil-kt:coil-compose:1.4.0")

}
