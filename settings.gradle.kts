pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.webrtc.org")
    }
}

rootProject.name = "Classroom-Quiz-Master"
include(":app")
