plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":common"))


    api(project(":util"))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
