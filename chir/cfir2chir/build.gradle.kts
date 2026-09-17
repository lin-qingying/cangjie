plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(project(":chir:chir-tree"))
    api(project(":cfir:cfir-tree"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
