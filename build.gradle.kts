import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinParcelize) apply false
    alias(libs.plugins.kotlinKapt) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    fun Project.configureLintBaseline() {
        extensions.findByType(CommonExtension::class.java)?.apply {
            lint {
                baseline = rootProject.file("lint-baseline.xml")
            }
        }
    }

    plugins.withId("com.android.application") {
        configureLintBaseline()
    }

    plugins.withId("com.android.library") {
        configureLintBaseline()
    }
}
