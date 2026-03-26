plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":resolution.common"))

    api(project(":cfir:cfir-tree"))
//    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-cones"))
//    api(project(":common:diagnostics"))
//    api(project(":cfir:checkers"))
    api(project(":cfir:symbols"))
    api(project(":common"))
    api(project(":util"))

    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter)
    testImplementation(testFixtures(project(":tests:test-infrastructure")))
    testRuntimeOnly(libs.junit.platform.launcher)
}
