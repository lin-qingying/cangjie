plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:cfir-common"))

    api(project(":cfir:cfir-cones"))
    api(project(":common:diagnostics"))
//    api(project(":cfir:checkers"))
    api(project(":common"))
    api(project(":util"))
    api(project(":cfir:semantics"))
    api(project(":cfir:providers"))

    implementation(libs.kotlinx.collections.immutable)
    api(project(":resolution.common"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testRuntimeOnly(libs.junit.platform.launcher)
}
