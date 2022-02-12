import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    android()

    val xcFramework = XCFramework("HackerNews")
    ios {
        binaries {
            framework {
                baseName = "HackerNews"
                xcFramework.add(this)
            }
        }
    }
    iosSimulatorArm64 {
        binaries {
            framework {
                baseName = "HackerNews"
                xcFramework.add(this)
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.coroutines.OptInAnnotation")
            }
        }

        val commonMain by getting {
            dependencies {
                api(CoRed.core)

                implementation(Coroutines.core)

                implementation(Ktor.core)
                implementation(Ktor.logging)
                implementation(Ktor.serialization)

                implementation(Serialization.json)

                implementation(Time.core)
                implementation(Result.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))

                implementation(Ktor.mock)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(Ktor.android)
                implementation(Ktor.okttp)

                implementation(AndroidX.lifeCycleViewModel)
            }
        }

        val androidTest by getting {
            dependencies {
                implementation(Coroutines.test)
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(Coroutines.core) {
                    version {
                        strictly(Coroutines.version)
                    }
                }
                implementation(Ktor.ios)
            }
        }

        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(Coroutines.core) {
                    version {
                        strictly(Coroutines.version)
                    }
                }
                implementation(Ktor.iosSimArm64)
            }
        }
    }
}

android {
    compileSdk = Android.compileSdkVersion

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
        minSdk = Android.minSdkVersion
        targetSdk = Android.targetSdkVersion
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

tasks {
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

