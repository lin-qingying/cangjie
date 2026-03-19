plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":common:diagnostics"))

    testImplementation(libs.junit.jupiter)
}
