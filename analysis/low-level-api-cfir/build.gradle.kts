plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    api(project(":analysis:analysis-api"))
    api(project(":psi"))
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:entrypoint"))
    api(project(":cfir:resolve"))
    api(project(":cfir:checkers"))
    api(project(":cfir:symbols"))
    api(project(":common:diagnostics"))
}
