import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.classroom.quizmaster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.classroom.quizmaster"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.classroom.quizmaster.QuizMasterTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        vectorDrawables.useSupportLibrary = true

        buildConfigField("String", "LAN_SERVICE_TYPE", "\"_quizmaster._tcp.\"")
        buildConfigField("String", "LAN_DEFAULT_HOST", "\"0.0.0.0\"")
        buildConfigField("int", "LAN_DEFAULT_PORT", "48765")
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjsr305=strict",
            "-Xjvm-default=all",
            "-Xcontext-receivers"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module",
                "DebugProbesKt.bin",
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/versions/9/**" // Added more general exclusion
            )
        }
    }

    lint {
        checkAllWarnings = true
        warningsAsErrors = true
        abortOnError = true
        sarifReport = true
        checkDependencies = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        unitTests.all {
            it.systemProperty(
                "robolectric.dependencyResolver",
                "org.robolectric.internal.dependency.GradleDependencyResolver"
            )
        }
        managedDevices {
            devices {
                maybeCreate<ManagedVirtualDevice>("pixel6Api34").apply {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp"
                }
            }
        }
    }

    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
    arg("dagger.hilt.disableModulesHaveInstallInCheck", "true")
}

hilt {
    enableAggregatingTask = true
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase.core)
    implementation(libs.firebase.appcheck)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.appcompat)
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.collection.ktx)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.base)
    implementation(libs.androidx.window)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.timber)
    implementation("org.slf4j:slf4j-nop:1.7.36") // Added SLF4J no-op
    implementation(libs.guava)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation(libs.compose.ui.util)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.foundation)
    implementation(libs.compose.foundation.layout)
    implementation(libs.compose.runtime)
    implementation(libs.compose.runtime.saveable)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.androidx.constraintlayout.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:${libs.versions.androidxLifecycle.get()}")

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.runtime)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation("androidx.paging:paging-common:${libs.versions.androidxPaging.get()}")

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime)
    implementation("androidx.work:work-multiprocess:${libs.versions.androidxWork.get()}")
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.cbor)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:${libs.versions.kotlinxSerialization.get()}")
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)

    implementation(libs.coil.compose)

    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.okio)

    implementation(libs.commons.io)
    implementation(libs.commons.text)
    implementation(libs.bouncycastle.bcprov)
    implementation("org.bouncycastle:bcpkix-jdk18on:${libs.versions.bouncycastle.get()}")

    implementation(libs.openpdf)
    implementation(libs.kotlin.csv)
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")


    coreLibraryDesugaring(libs.android.desugar.jdk.libs)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.truth)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.robolectricAndroidAllApi14)
    testImplementation(libs.robolectricAndroidAllInstrumentedApi14)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation("androidx.paging:paging-testing:${libs.versions.androidxPaging.get()}")
    testImplementation(libs.androidx.work.testing)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.firebase.bom))
    androidTestImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.contrib)
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:${libs.versions.espresso.get()}")
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.espresso.web)
    androidTestImplementation(libs.androidx.test.espresso.idling)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.androidx.uiautomator)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation("androidx.paging:paging-testing:${libs.versions.androidxPaging.get()}")
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.lifecycle.runtime.testing)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.mockk)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)

    androidTestUtil("androidx.test:orchestrator:1.5.0")

    kspAndroidTest(libs.hilt.compiler)
    kspAndroidTest(libs.androidx.hilt.compiler)
}

configurations.configureEach {
    exclude(group = "com.google.protobuf", module = "protobuf-lite")
    exclude(group = "org.apache.commons", module = "commons-text")
    exclude(group = "org.slf4j", module = "slf4j-simple") // Exclude slf4j-simple
    resolutionStrategy.force("com.google.protobuf:protobuf-javalite:3.21.12")
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}
