plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))

    api(project(":resolution.common"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:providers"))
    api(project(":cfir:providers"))
    implementation(libs.kotlinx.collections.immutable)
    api(project(":common"))
}
