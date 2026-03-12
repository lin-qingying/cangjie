plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:config"))
    compileOnly(intellijCore())
    testImplementation(libs.junit.jupiter)
}
