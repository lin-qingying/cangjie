plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))

    api(project(":cfir:constraint-system"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:symbols"))
    implementation(libs.kotlinx.collections.immutable)
    api(project(":common"))
}
