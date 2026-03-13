plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:diagnostics"))
    api(project(":cfir:cfir-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
