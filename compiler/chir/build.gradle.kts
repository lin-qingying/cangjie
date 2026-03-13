plugins {
    kotlin("jvm")
}

val requireExternalReferenceRepo = providers.environmentVariable("CI")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

dependencies {
    implementation(project(":flatbuffers-gen"))
    implementation(libs.flatbuffers.java)

    testImplementation(project(":compiler:codegen"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.register("verifyExternalReferenceRepo") {
    group = "verification"
    description = "Ensures external/cangjie_compiler exists and is non-empty when running in CI."
    onlyIf { requireExternalReferenceRepo.get() }
    doLast {
        val repoRoot = rootProject.projectDir
        val externalRepo = repoRoot.resolve("external/cangjie_compiler")
        require(externalRepo.exists() && externalRepo.isDirectory) {
            "Missing required reference repository: ${externalRepo.absolutePath}"
        }
        val hasFiles = externalRepo.walkTopDown().any { it.isFile }
        require(hasFiles) {
            "Reference repository is empty: ${externalRepo.absolutePath}"
        }
    }
}

tasks.named("check") {
    dependsOn("verifyExternalReferenceRepo")
}
