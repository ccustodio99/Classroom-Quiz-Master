plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.acme.quizmaster.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
