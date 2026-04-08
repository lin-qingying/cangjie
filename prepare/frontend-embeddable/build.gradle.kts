import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    `java-library`
}

description = "仓颉前端 embeddable 公开门面工件。"

val bundledProjectPaths = listOf(
    ":compiler:frontend",
    ":dependencies:intellij-core",
)

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    dependsOn(bundledProjectPaths.map { "$it:jar" })
    from({
        bundledProjectPaths.flatMap { projectPath ->
            project(projectPath)
                .configurations
                .getByName("runtimeClasspath")
                .resolvedConfiguration
                .resolvedArtifacts
                .map { artifact -> zipTree(artifact.file) }
        }
    })
    manifest.attributes["Implementation-Title"] = "cangjie-frontend-embeddable"
    manifest.attributes["Implementation-Version"] = project.version.toString()
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    dependsOn(bundledProjectPaths.map { "$it:jar" })
    from({
        val sourceProjectPaths = linkedSetOf<String>()
        bundledProjectPaths.forEach { projectPath ->
            sourceProjectPaths += projectPath
            sourceProjectPaths += project(projectPath)
                .configurations
                .getByName("runtimeClasspath")
                .incoming
                .resolutionResult
                .allComponents
                .mapNotNull { component -> (component.id as? ProjectComponentIdentifier)?.projectPath }
        }

        sourceProjectPaths
            .map(::project)
            .filter { candidate -> candidate.plugins.hasPlugin("java-base") }
            .distinctBy { candidate -> candidate.path }
            .map { dependencyProject ->
                dependencyProject.extensions.getByType<JavaPluginExtension>().sourceSets.getByName("main").allSource
            }
    })
}
