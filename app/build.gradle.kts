plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.acme.quizmaster.MainKt")
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
