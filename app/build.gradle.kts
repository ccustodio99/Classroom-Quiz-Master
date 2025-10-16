plugins {
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.acme.quizmaster.MainKt")
}
