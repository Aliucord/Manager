val accompanistVersion = "0.26.0-alpha"
val composeVersion = "1.3.0-alpha02"
val ktorVersion = "2.0.3"

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.google.devtools.ksp") version "1.7.10-1.0.6"
    kotlin("plugin.serialization") version "1.7.10"
}

android {
    namespace = "com.aliucord.manager"
    compileSdk = 33

    defaultConfig {
        minSdk = 24
        targetSdk = 33
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
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    applicationVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }

    buildFeatures.compose = true
    composeOptions.kotlinCompilerExtensionVersion = "1.3.0-rc01"
}

dependencies {
    implementation(fileTree("./libs"))

    // core dependencies
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("com.android:zipflinger:7.4.0-alpha09")
//    implementation("com.github.Aliucord:libzip:1.0.0")

    // androidX activity
    implementation("androidx.activity:activity-compose:1.6.0-alpha05")

    // compose dependencies
    implementation("androidx.compose.ui:ui:${composeVersion}")
    implementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    implementation("androidx.compose.material:material-icons-extended:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.0.0-alpha15")
    implementation("androidx.paging:paging-compose:1.0.0-alpha15")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0-alpha01")

    // accompanist dependencies
    implementation("com.google.accompanist:accompanist-systemuicontroller:${accompanistVersion}")
    implementation("com.google.accompanist:accompanist-permissions:${accompanistVersion}")

    // other dependencies
    implementation("io.coil-kt:coil-compose:2.1.0")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("de.upb.cs.swt:axml:2.1.2") // 2.1.2 is broken btw
    implementation("com.android.tools.build:apksig:7.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // taxi
    implementation("com.github.X1nto:Taxi:1.0.0")

    // ktor
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}
