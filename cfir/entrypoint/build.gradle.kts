plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))
    api(project(":cfir:cfir-tree"))
    api(project(":cfir:symbols"))
    api(project(":cfir:resolve"))
    api(project(":cfir:diagnostics"))
    api(project(":cfir:cfir-serialization"))
    api(project(":cfir:checkers"))
    api(project(":cfir:raw-cfir:psi2cfir"))
    api(project(":psi"))
    api(project(":compiler:config"))
    api(project(":common"))
    api(project(":util"))

    compileOnly(intellijCore())
}
