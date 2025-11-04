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
    }
}

rootProject.name = "Classroom-Quiz-Master"
// Legacy :app module is no longer part of the build; delegate to the new android/ project instead.
includeBuild("android")
