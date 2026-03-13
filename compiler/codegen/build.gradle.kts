plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":compiler:chir"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.register<Test>("parityCheck") {
    group = "verification"
    description = "Runs CHIR to LLVM parity checks against baseline expectations."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("parity")
    }
    systemProperty("parity.maxCriticalDiffs", project.findProperty("parity.maxCriticalDiffs")?.toString() ?: "0")
}

tasks.named("check") {
    dependsOn("parityCheck")
}
