plugins {
    id("com.android.application")
    id("kotlin-android")
}

repositories {
    exclusiveContent {
        forRepository {
            flatDir { dir(project.file("libs")) }
        }
        filter {
            includeGroup("com.github.kittinunf.hackernews")
        }
    }
}

android {
    compileSdk = Android.compileSdkVersion

    defaultConfig {
        applicationId = "com.github.kittinunf.app.knews"
        minSdk = Android.minSdkVersion
        targetSdk = Android.targetSdkVersion
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = AndroidX.Versions.compose
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true")
    }
}

dependencies {
    // link hackernews lib and dependencies
    // add this instead if you want to consume in the binary format (aar) (debug or release)
    // implementation(group = "com.github.kittinunf.hackernews", name = "hackernews-debug", version = "+")
    implementation(project(":libs:hackernews"))
    implementation(Coroutines.core)
    implementation(Ktor.core)
    implementation(Ktor.logging)
    implementation(Ktor.okttp)
    implementation(Serialization.json)
    implementation(Time.core)

    // core
    implementation(Android.material)
    implementation(AndroidX.coreKtx)
    coreLibraryDesugaring(Android.desugarJdkLibs)

    // compose
    implementation(Android.accompanist)
    implementation(AndroidX.composeActivity)
    implementation(AndroidX.composeMaterial)
    implementation(AndroidX.composeTooling)
    implementation(AndroidX.composeUi)
    implementation(AndroidX.composeViewModel)

    // lifecycle
    implementation(AndroidX.lifeCycleViewModel)
}
