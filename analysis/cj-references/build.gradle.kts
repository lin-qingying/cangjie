plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:analysis-internal-utils"))
    implementation(project(":psi"))
}
