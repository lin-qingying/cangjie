plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:providers"))
    api(project(":cfir:resolve"))
    api(project(":common:diagnostics"))
    api(project(":cfir:cfir-serialization"))
    api(project(":cfir:checkers"))
    api(project(":cfir:raw-cfir:psi2cfir"))
    api(project(":cfir:raw-cfir:light-tree2cfir"))
    api(project(":psi"))
    api(project(":compiler:config"))
    api(project(":common"))
    api(project(":util"))

    compileOnly(intellijCore())
}
