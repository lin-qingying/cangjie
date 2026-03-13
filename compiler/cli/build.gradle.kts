plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:config"))
    implementation(project(":compiler:chir"))
    implementation(project(":compiler:codegen"))
    compileOnly(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
