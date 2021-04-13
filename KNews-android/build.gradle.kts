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
    compileSdk = 30
    buildToolsVersion = "30.0.3"

    defaultConfig {
        applicationId = "com.github.kittinunf.app.knews"
        minSdk = 24
        targetSdk = 30
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.0.0-beta01"
    }
}

dependencies {
    // link hackernews lib and dependencies
    // add this instead if you want to consume and the binary format (aar) (debug or release)
    // implementation(group = "com.github.kittinunf.hackernews", name = "hackernews-debug", version = "+")
    implementation(project(":libs:hackernews"))
    implementation(project(":libs:redux"))
    implementation(Coroutines.core)
    implementation(Coroutines.android)
    implementation(Ktor.core)
    implementation(Ktor.logging)
    implementation(Ktor.android)
    implementation(Ktor.okttp)
    implementation(Serialization.json)
    implementation(Time.core)

    // core
    implementation(Android.material)
    implementation(AndroidX.coreKtx)

    // compose
    implementation(AndroidX.composeActivity)
    implementation(AndroidX.composeUi)
    implementation(AndroidX.composeMaterial)
    implementation(AndroidX.composeTooling)
    implementation(AndroidX.composeViewModel)

    // lifecycle
    implementation(AndroidX.lifeCycleViewModel)
    implementation(AndroidX.lifeCycleRuntime)
}
