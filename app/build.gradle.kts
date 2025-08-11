@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
}

val isRelease = System.getenv("RELEASE")?.toBoolean() ?: false
val gitCurrentBranch = providers.execIgnoreCode("git", "symbolic-ref", "--quiet", "--short", "HEAD").takeIf { it.isNotEmpty() }
val gitLatestCommit = providers.execIgnoreCode("git", "rev-parse", "--short", "HEAD")
val gitHasLocalCommits = gitCurrentBranch?.let { branch ->
    val remoteBranchExists = providers.execIgnoreCode("git", "ls-remote", "--heads", "origin", branch)
        .isNotEmpty()

    remoteBranchExists && providers.execIgnoreCode("git", "log", "origin/$branch..HEAD").isNotEmpty()
} ?: false
val gitHasHasLocalChanges = providers.execIgnoreCode("git", "status", "-s").isNotEmpty()

android {
    namespace = "com.aliucord.manager"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
        versionCode = 10_00_03
        versionName = "1.0.3"

        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "TAG", "\"AliucordManager\"")
        buildConfigField("String", "SUPPORT_SERVER", "\"EsNDvBaHVU\"")

        buildConfigField("String", "BACKEND_URL", "\"https://aliucord.com/\"")

        buildConfigField("Boolean", "RELEASE", isRelease.toString())
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
            storeFile = System.getenv("SIGNING_STORE_FILE")?.let(::File)
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
        }
    }

    buildTypes {
        val isRelease = System.getenv("RELEASE")?.toBoolean() ?: false
        val hasReleaseSigning = System.getenv("SIGNING_STORE_PASSWORD")?.isNotEmpty() == true

        if (isRelease && !hasReleaseSigning)
            error("Missing keystore in a release workflow!")

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

        create("staging") {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = signingConfigs.getByName("debug")
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
                // Reflection symbol list (https://stackoverflow.com/a/41073782/13964629)
                add("/**/*.kotlin_builtins")
            }
        }
    }

    packaging {
        resources {
            // okhttp3 is used by some lib (no cookies so publicsuffixes.gz can be dropped)
            excludes += "/okhttp3/**"

            // Remnants of smali/baksmali lib
            excludes += "/*.properties"
            excludes += "/org/antlr/**"
            excludes += "/com/android/tools/smali/**"
            excludes += "/org/eclipse/jgit/**"

            // bouncycastle
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "/org/bouncycastle/**"
        }
        jniLibs {
            // x86 is dead
            excludes += "/lib/x86/*.so"

            // Equivalent of AndroidManifest's extractNativeLibs=false
            useLegacyPackaging = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    lint {
        disable += "ModifierParameter"
    }
}

kotlin {
    sourceSets.all {
        languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
    compilerOptions {
        val reportsDir = layout.buildDirectory.asFile.get()
            .resolve("reports").absolutePath

        jvmTarget = JvmTarget.JVM_1_8
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${reportsDir}",
            "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode", // @StringRes in field parameters of a class warning
        )
    }
}

tasks.withType<JavaCompile> {
    // Disable warnings about obsolete target version
    options.compilerArgs.add("-Xlint:-options")
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

    implementation(libs.kotlinx.immutable)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.apksig)
    implementation(libs.axml)
    implementation(libs.bouncycastle)
    implementation(libs.binaryResources)
    implementation(libs.diff)
    implementation(libs.microg)
    implementation(libs.smali)
    implementation(libs.baksmali)
    implementation(libs.compose.pipette)
    implementation(libs.compose.shimmer)
    implementation(libs.zip)

    coreLibraryDesugaring(libs.desugaring)
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
