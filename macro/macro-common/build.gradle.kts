plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":flatbuffers-gen"))
    api(project(":common"))
    api(project(":cfir:cfir-tree"))
    api(project(":psi"))
    implementation(libs.flatbuffers.java)

    compileOnly(intellijCore())
}
