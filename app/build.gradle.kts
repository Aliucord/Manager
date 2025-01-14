@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val gitCurrentBranch = providers.execIgnoreCode("git", "symbolic-ref", "--short", "HEAD")
val gitLatestCommit = providers.execIgnoreCode("git", "rev-parse", "--short", "HEAD")
val gitHasLocalCommits = providers.execIgnoreCode("git", "log", "origin/$gitCurrentBranch..HEAD").isNotEmpty()
val gitHasHasLocalChanges = providers.execIgnoreCode("git", "status", "-s").isNotEmpty()

android {
    namespace = "com.aliucord.manager"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.0.1"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TAG", "\"AliucordManager\"")
        buildConfigField("String", "SUPPORT_SERVER", "\"EsNDvBaHVU\"")

        buildConfigField("String", "BACKEND_URL", "\"https://aliucord.com/\"")

        buildConfigField("String", "GIT_BRANCH", "\"$gitCurrentBranch\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitLatestCommit\"")
        buildConfigField("boolean", "GIT_LOCAL_COMMITS", "$gitHasLocalCommits")
        buildConfigField("boolean", "GIT_LOCAL_CHANGES", "$gitHasHasLocalChanges")
    }

    signingConfigs {
        create("release") {
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            storeFile = System.getenv("SIGNING_STORE_FILE")?.let { File(it) }
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidComponents {
        onVariants(selector().withBuildType("release")) {
            it.packaging.resources.excludes.apply {
                // Debug metadata
                add("/**/*.version")
                add("/kotlin-tooling-metadata.json")
                // Kotlin debugging (https://github.com/Kotlin/kotlinx.coroutines/issues/2274)
                add("/DebugProbesKt.bin")
            }
        }
    }

    packaging {
        resources {
            // Reflection symbol list (https://stackoverflow.com/a/41073782/13964629)
            excludes += "/**/*.kotlin_builtins"

            // okhttp3 is used by some lib (no cookies so publicsuffixes.gz can be dropped)
            excludes += "/okhttp3/**"

            // Remnants of smali/baksmali lib
            excludes += "/*.properties"
            excludes += "/org/antlr/**"
            excludes += "/com/android/tools/smali/**"
            excludes += "/org/eclipse/jgit/**"

            // Other
            excludes += "/org/bouncycastle/**"
        }
        jniLibs {
            // x86 is dead
            excludes += "/lib/x86/*.so"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        val reportsDir = layout.buildDirectory.asFile.get()
            .resolve("reports").absolutePath

        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${reportsDir}",
        )
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        disable += "ModifierParameter"
    }
}

dependencies {
    implementation(libs.bundles.accompanist)
    implementation(libs.bundles.androidx)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.compose)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.ktor)
    implementation(libs.bundles.voyager)

    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.runtime.tracing)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.immutable)

    implementation(libs.apksig)
    implementation(libs.axml)
    implementation(libs.bouncycastle)
    implementation(libs.binaryResources)
    implementation(libs.diff)
    implementation(libs.microg)
    implementation(libs.smali)
    implementation(libs.baksmali)
    implementation(variantOf(libs.zip) { artifactType("aar") })
}

fun ProviderFactory.execIgnoreCode(vararg command: String): String = run {
    val result = exec {
        commandLine = command.toList()
        isIgnoreExitValue = true
    }

    val stderr = result.standardError.asText.get()
    if (stderr.isNotEmpty())
        throw RuntimeException(stderr)

    result.standardOutput.asText.get().trim()
}
