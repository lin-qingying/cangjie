plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())
    implementation(project(":common:diagnostics"))
    implementation(project(":psi"))

    testImplementation(intellijCore())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}
