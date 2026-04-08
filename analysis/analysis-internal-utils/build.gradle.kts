plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":psi"))
}
