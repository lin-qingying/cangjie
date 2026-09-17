plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    api(project(":chir:chir-tree"))
    implementation(libs.asm)
    testImplementation(project(":chir:cfir2chir"))
    testImplementation(libs.asm.util)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
