plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

// this is workaround to make it work with Kotlin 1.4, it will not be needed anymore in Kotlin 1.5
android {
    configurations {
        create("androidTestApi")
        create("androidTestDebugApi")
        create("androidTestReleaseApi")
        create("testApi")
        create("testDebugApi")
        create("testReleaseApi")
    }
}

kotlin {
    android()
    ios { // comment/uncomment this for building both the arm64 + x64 framework
//    iosX64("ios") { // comment/uncomment this for just building the x64 framework
        binaries {
            framework {
                baseName = "HackerNews"
            }
        }
    }
    sourceSets {
        all {
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.time.ExperimentalTime")
                useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
                useExperimentalAnnotation("kotlinx.coroutines.OptInAnnotation")
            }
        }

        val commonMain by getting {
            dependencies {
                api(project(":libs:redux"))

                implementation(Coroutines.core)

                implementation(Ktor.core)
                implementation(Ktor.logging)
                implementation(Ktor.serialization)

                implementation(Serialization.json)

                implementation(Time.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(Kotlin.testCommon)
                implementation(Kotlin.testAnnotationsCommon)

                implementation(Ktor.mock)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Coroutines.android)

                implementation(Ktor.android)
                implementation(Ktor.okttp)

                implementation(AndroidX.lifeCycleViewModel)
            }
        }

        val androidTest by getting {
            dependencies {
                implementation(Coroutines.test)

                implementation(Kotlin.testJunit)
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(Ktor.ios)
            }
        }
    }
}

android {
    compileSdkVersion(Android.compileSdkVersion)
    buildToolsVersion(Android.buildToolsVersion)

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            java.srcDirs("src/androidMain/kotlin")
            res.srcDirs("src/androidMain/res")
        }

        getByName("androidTest") {
            manifest.srcFile("src/androidTest/AndroidManifest.xml")
            java.srcDirs("src/androidTest/kotlin")
            res.srcDirs("src/androidTest/res")
        }
    }

    defaultConfig {
        minSdkVersion(Android.minSdkVersion)
        targetSdkVersion(Android.targetSdkVersion)
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks {
    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    fun getFrameworks(buildType: String): List<String> {
        val arm64 = project.buildDir.resolve("bin/iosArm64/${buildType}Framework/HackerNews.framework").toString()
        val x64 = project.buildDir.resolve("bin/iosx64/${buildType}Framework/HackerNews.framework").toString()
        return listOf(
            "-framework", arm64,
            "-debug-symbols", "$arm64.dSYM",
            "-framework", x64,
            "-debug-symbols", "$x64.dSYM"
        )
    }

    fun getTaskCommand(buildType: String, outputDir: File): List<String> {
        val name = "HackerNews-$buildType.xcframework"
        return listOf("xcodebuild", "-create-xcframework") + getFrameworks(buildType) + listOf("-output", File("$outputDir/$name").toString())
    }

    val createXCFramework by registering {
        val buildTypes = listOf("debug", "release")
        val output = project.buildDir.resolve("bin/ios")

        if (output.exists()) project.delete(output)

        doLast {
            buildTypes.forEach { buildType ->
                project.exec {
                    commandLine = getTaskCommand(buildType, output)
                }
            }
        }
    }
}

