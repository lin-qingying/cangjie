plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:cli:cli-base"))
    api(project(":compiler:frontend"))
    implementation(project(":cfir:entrypoint"))
    implementation(project(":cfir:providers"))
    implementation(project(":chir:cjmp-linker"))
    implementation(project(":compiler:codegen"))
    implementation(project(":compiler:config"))
    implementation(project(":common:diagnostics"))

    compileOnly(intellijCore())
    runtimeOnly(intellijCore())

    testImplementation(project(":cfir:entrypoint"))
    testImplementation(project(":flatbuffers-gen"))
    testImplementation(libs.flatbuffers.java)
    testImplementation(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
