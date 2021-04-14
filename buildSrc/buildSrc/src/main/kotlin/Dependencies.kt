object Kotlin {

    private const val version = "1.4.31"

    const val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val pluginSerialization = "org.jetbrains.kotlin:kotlin-serialization:$version"

    const val testCommon = "org.jetbrains.kotlin:kotlin-test-common"
    const val testAnnotationsCommon = "org.jetbrains.kotlin:kotlin-test-annotations-common"
    const val testJunit = "org.jetbrains.kotlin:kotlin-test-junit"
}

object Android {

    const val minSdkVersion = 24
    const val targetSdkVersion = 30
    const val compileSdkVersion = 30

    const val buildToolsVersion = "30.0.3"

    private const val version = "7.0.0-alpha14"
    const val plugin = "com.android.tools.build:gradle:$version"

    private const val materialVersion = "1.3.0"
    const val material = "com.google.android.material:material:$materialVersion"
}

object AndroidX {

    object Versions {
        const val test = "1.2.0"
        const val jUnit = "1.1.1"
        const val core = "1.3.2"
        const val lifeCycle = "2.3.1"
        const val compose = "1.0.0-beta01"
        const val composeActivity = "1.3.0-alpha05"
        const val composeViewModel = "1.0.0-alpha03"
    }

    const val coreKtx = "androidx.core:core-ktx:${Versions.core}"
    const val composeUi = "androidx.compose.ui:ui:${Versions.compose}"
    const val composeMaterial = "androidx.compose.material:material:${Versions.compose}"
    const val composeTooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
    const val composeViewModel = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.composeViewModel}"
    const val composeActivity = "androidx.activity:activity-compose:${Versions.composeActivity}"
    const val lifeCycleViewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifeCycle}"
    const val lifeCycleRuntime = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifeCycle}"
    const val testRules = "androidx.test:rules:${Versions.test}"
    const val testJunit = "androidx.test.ext:junit:${Versions.jUnit}"
}

object Coroutines {

    private const val mtVersion = "1.4.3-native-mt"
    private const val version = "1.4.3"

    const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$mtVersion"
    const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"

    const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
}

object Serialization {

    private const val version = "1.0.1"

    const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:$version"
}

object Time {

    private const val version = "0.1.1"

    const val core = "org.jetbrains.kotlinx:kotlinx-datetime:$version"
}

object Ktor {

    private const val version = "1.4.3"

    const val core = "io.ktor:ktor-client-core:$version"
    const val android = "io.ktor:ktor-client-android:$version"
    const val okttp = "io.ktor:ktor-client-okhttp:$version"
    const val ios = "io.ktor:ktor-client-ios:$version"

    const val serialization = "io.ktor:ktor-client-serialization:$version"

    const val logging = "io.ktor:ktor-client-logging:$version"
    const val mock = "io.ktor:ktor-client-mock:$version"
}

// Test libraries
object JUnit {

    private const val version = "4.13.1"
    private const val jacocoVersion = "0.16.0"

    const val jvm = "junit:junit:$version"
    const val pluginJacoco = "gradle.plugin.com.vanniktech:gradle-android-junit-jacoco-plugin:$jacocoVersion"
}

object Jacoco {
    const val version = "0.8.6"
}
