import org.gradle.api.tasks.Delete

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinParcelize) apply false
    alias(libs.plugins.kotlinKapt) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.baselineProfile) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
