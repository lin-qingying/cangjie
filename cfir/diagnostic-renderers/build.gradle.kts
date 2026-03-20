plugins {
    kotlin("jvm")
}
sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { none() }
}


dependencies {
    api(project(":cfir:cfir-cones"))

    api(project(":common:diagnostics"))
    api(project(":cfir:cfir-tree"))




    compileOnly(intellijCore())
}
