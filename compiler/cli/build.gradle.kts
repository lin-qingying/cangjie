plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:cli:cli-base"))
    api(project(":compiler:frontend"))
    implementation(project(":compiler:config"))
    implementation(project(":common:diagnostics"))

    runtimeOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
