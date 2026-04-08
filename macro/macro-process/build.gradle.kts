plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":macro:macro-common"))

    compileOnly(intellijCore())
}
