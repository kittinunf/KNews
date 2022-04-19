object Kotlin {

    private const val version = "1.6.10"

    const val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    const val pluginSerialization = "org.jetbrains.kotlin:kotlin-serialization:$version"
}

object Android {

    const val minSdkVersion = 24
    const val targetSdkVersion = 31
    const val compileSdkVersion = 31

    private const val version = "7.1.1"
    const val plugin = "com.android.tools.build:gradle:$version"

    private const val materialVersion = "1.5.0"
    const val material = "com.google.android.material:material:$materialVersion"

    private const val accompanistVersion = "0.23.0"
    const val accompanist = "com.google.accompanist:accompanist-swiperefresh:$accompanistVersion"

    private const val desugarJdkLibsVersion = "1.1.5"
    const val desugarJdkLibs = "com.android.tools:desugar_jdk_libs:$desugarJdkLibsVersion"
}

object AndroidX {

    object Versions {
        const val core = "1.7.0"
        const val compose = "1.2.0-alpha07"
        const val composeActivity = "1.4.0"
    }

    // Core
    const val coreKtx = "androidx.core:core-ktx:${Versions.core}"

    // Compose
    const val composeActivity = "androidx.activity:activity-compose:${Versions.composeActivity}"
    const val composeMaterial = "androidx.compose.material:material:${Versions.compose}"
    const val composeTooling = "androidx.compose.ui:ui-tooling:${Versions.compose}"
    const val composeUi = "androidx.compose.ui:ui:${Versions.compose}"
}

object Coroutines {

    const val version = "1.6.0-native-mt"

    const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
    const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
}

object Serialization {

    private const val version = "1.3.1"

    const val json = "org.jetbrains.kotlinx:kotlinx-serialization-json:$version"
}

object Time {

    private const val version = "0.3.2"

    const val core = "org.jetbrains.kotlinx:kotlinx-datetime:$version"
}

object Ktor {

    private const val version = "1.6.7"

    const val core = "io.ktor:ktor-client-core:$version"

    const val okttp = "io.ktor:ktor-client-okhttp:$version"
    const val android = "io.ktor:ktor-client-android:$version"

    const val ios = "io.ktor:ktor-client-ios:$version"
    const val iosSimArm64 = "io.ktor:ktor-client-ios-iossimulatorarm64:$version"

    const val serialization = "io.ktor:ktor-client-serialization:$version"

    const val logging = "io.ktor:ktor-client-logging:$version"
    const val mock = "io.ktor:ktor-client-mock:$version"
}

object CoRed {

    private const val version = "0.7.0"

    const val core = "com.github.kittinunf.cored:cored:$version"
}

object Result {

    private const val version = "5.2.1"

    const val core = "com.github.kittinunf.result:result:$version"
}

// Test libraries
object JUnit {

    private const val version = "4.13.2"

    const val jvm = "junit:junit:$version"
}

object Jacoco {

    const val version = "0.8.7"
}
