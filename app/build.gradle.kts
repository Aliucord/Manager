/*
 * Copyright (c) 2021 Juby210
 * Licensed under the Open Software License version 3.0
 */

val accompanistVersion = "0.20.0"
val composeVersion = "1.0.4"
val ktorVersion = "1.6.4"

plugins {
    id("com.android.application")
    id("kotlin-android")
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

    packagingOptions {
        resources {
            excludes.add("META-INF/*.kotlin_module")
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
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("com.google.android.material:material:1.4.0")

    // compose dependencies
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material:material:$composeVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.0-rc01")
    implementation("androidx.navigation:navigation-compose:2.4.0-alpha10")

    // Ktor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")

    // accompanist dependencies
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion")

    // other dependencies
    implementation("com.google.code.gson:gson:2.8.8")
}
