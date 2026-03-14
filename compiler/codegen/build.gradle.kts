plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

dependencies {
    implementation(project(":compiler:chir"))
    implementation(project(":llvm-interop:llvm-interop-jni"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.register<Test>("parityCheck") {
    group = "verification"
    description = "Runs CHIR to LLVM parity checks against baseline expectations."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeEngines("junit-vintage", "junit-jupiter")
    }
    filter {
        includeTestsMatching("org.cangnova.cangjie.codegen.parity.*")
    }
    systemProperty("parity.maxCriticalDiffs", project.findProperty("parity.maxCriticalDiffs")?.toString() ?: "0")
}

tasks.named("check") {
    dependsOn("parityCheck")
}

projectTests {
    testGenerator("org.cangnova.cangjie.codegen.parity.TestGeneratorForCodegenParity")
}
