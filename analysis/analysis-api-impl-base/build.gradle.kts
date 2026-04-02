plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":psi"))
    compileOnly(intellijCore())
}
