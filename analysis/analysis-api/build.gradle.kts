plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    api(project(":psi"))
}
