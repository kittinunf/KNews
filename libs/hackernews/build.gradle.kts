import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    androidTarget()

    val xcFramework = XCFramework("HackerNews")
    iosX64 {
        binaries {
            framework {
                baseName = "HackerNews"
                xcFramework.add(this)
            }
        }
    }
    iosArm64 {
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

        commonMain {
            dependencies {
                api(CoRed.core)

                implementation(Coroutines.core)

                implementation(Ktor.core)
                implementation(Ktor.content)
                implementation(Ktor.json)
                implementation(Ktor.logging)
                implementation(Ktor.serialization)

                implementation(Serialization.json)

                implementation(Time.core)
                implementation(Result.core)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(Coroutines.test)
                implementation(Ktor.mock)
            }
        }

        jvmMain {
            dependencies {
                implementation(Ktor.android)
                implementation(Ktor.okttp)
            }
        }

        androidMain {
            val jvmMain by getting
            dependsOn(jvmMain)
        }

        val androidUnitTest by getting {
            dependencies {
            }
        }

        iosMain {
            val iosSimulatorArm64Main by getting
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                implementation(Coroutines.core) {
                    version { strictly(Coroutines.version) }
                }
                implementation(Ktor.ios)
            }
        }
    }

    jvmToolchain(17)
}

android {
    compileSdk = Android.compileSdkVersion
    namespace = "com.github.kittinunf.libs"

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
