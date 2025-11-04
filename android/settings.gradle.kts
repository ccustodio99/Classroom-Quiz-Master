pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "ClassroomLMS"

include(":app")
include(":core:model")
include(":core:common")
include(":core:database")
include(":core:network")
include(":core:sync")
include(":feature:auth")
include(":feature:home")
include(":feature:learn")
include(":feature:classroom")
include(":feature:activity")
include(":feature:profile")
include(":feature:live")

