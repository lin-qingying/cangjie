plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    api(project(":analysis:analysis-api"))
}
