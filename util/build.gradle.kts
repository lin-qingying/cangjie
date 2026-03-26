plugins {
    kotlin("jvm")
    id("common-configuration")
}

dependencies {
    compileOnly(intellijCore())
    testImplementation(libs.junit.jupiter)
}

