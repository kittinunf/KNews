pluginManagement {
    repositories {
        jcenter()
        google()
        mavenCentral()

        maven { setUrl("https://dl.bintray.com/kotlin/kotlin") }
        maven { setUrl("https://kotlin.bintray.com/kotlinx") }
    }
}

// products
include("KNews-android")

// libraries
include("libs:hackernews")
