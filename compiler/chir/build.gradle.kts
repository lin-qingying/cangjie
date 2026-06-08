import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    kotlin("jvm")
}

abstract class VerifyExternalReferenceRepoTask : DefaultTask() {
    @get:Input
    abstract val required: Property<Boolean>

    @get:Optional
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val externalRepoDir: DirectoryProperty

    @TaskAction
    fun verify() {
        if (!required.get()) return

        val externalRepo = externalRepoDir.get().asFile
        if (!externalRepo.exists() || !externalRepo.isDirectory) {
            throw GradleException("Missing required reference repository: ${externalRepo.absolutePath}")
        }

        val hasFiles = externalRepo.walkTopDown().any { it.isFile }
        if (!hasFiles) {
            throw GradleException("Reference repository is empty: ${externalRepo.absolutePath}")
        }
    }
}

val requireExternalReferenceRepo = providers.environmentVariable("CI")
    .map { it.equals("true", ignoreCase = true) }
    .orElse(false)

dependencies {
    api(project(":cfir:cfir-tree"))
    implementation(project(":flatbuffers-gen"))
    implementation(libs.flatbuffers.java)

    testImplementation(project(":compiler:codegen"))
    testImplementation(project(":compiler:jvm-codegen"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

val verifyExternalReferenceRepo = tasks.register<VerifyExternalReferenceRepoTask>("verifyExternalReferenceRepo") {
    group = "verification"
    description = "Ensures external/cangjie_compiler exists and is non-empty when running in CI."
    required.set(requireExternalReferenceRepo)
    externalRepoDir.set(rootProject.layout.projectDirectory.dir("external/cangjie_compiler"))
}

tasks.named("check") {
    dependsOn(verifyExternalReferenceRepo)
}
