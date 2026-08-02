plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":analysis:analysis-api-cfir"))
    implementation(project(":code-insight:api"))
    implementation(project(":code-insight:override-implement"))

    testImplementation(intellijCore())
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
