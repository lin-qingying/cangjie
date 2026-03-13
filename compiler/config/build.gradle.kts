plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
}
