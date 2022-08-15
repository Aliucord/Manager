plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
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
            isShrinkResources = true
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

    buildFeatures.compose = true
    composeOptions.kotlinCompilerExtensionVersion = "1.3.0"
}

dependencies {
    implementation(fileTree("./libs"))

    // core dependencies
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("com.android:zipflinger:7.4.0-alpha09")
//    implementation("com.github.Aliucord:libzip:1.0.0")

    // androidX activity
    implementation("androidx.activity:activity-compose:1.6.0-beta01")

    // compose dependencies
    val composeVersion = "1.3.0-alpha03"

    implementation("androidx.compose.ui:ui:${composeVersion}")
    implementation("androidx.compose.ui:ui-tooling:${composeVersion}")
    implementation("androidx.compose.material:material-icons-extended:${composeVersion}")
    implementation("androidx.compose.material3:material3:1.0.0-alpha16")
    implementation("androidx.paging:paging-compose:1.0.0-alpha16")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.0-alpha01")

    // accompanist dependencies
    val accompanistVersion = "0.26.1-alpha"
    implementation("com.google.accompanist:accompanist-systemuicontroller:${accompanistVersion}")
    implementation("com.google.accompanist:accompanist-permissions:${accompanistVersion}")

    // Koin
    val koinVersion = "3.2.0"
    implementation("io.insert-koin:koin-android:$koinVersion")
    implementation("io.insert-koin:koin-androidx-compose:$koinVersion")

    // other dependencies
    implementation("io.coil-kt:coil-compose:2.1.0")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("de.upb.cs.swt:axml:2.1.3") // 2.1.2 is broken btw
    implementation("com.android.tools.build:apksig:7.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    // taxi
    implementation("com.github.X1nto:Taxi:1.1.0")

    // ktor
    val ktorVersion = "2.1.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}
