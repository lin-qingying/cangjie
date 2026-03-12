plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":cfir:cfir-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
