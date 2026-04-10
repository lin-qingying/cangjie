import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    `java-library`
}

description = "仓颉前端测试基础设施公开门面工件。"

val fixtureProjectPaths = listOf(
    ":tests:test-infrastructure",
)

tasks.named<Jar>("jar") {
    fixtureProjectPaths.forEach { dependsOn("$it:testFixturesJar") }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    exclude("META-INF/maven/**")
    from({
        fixtureProjectPaths.map { projectPath ->
            zipTree(project(projectPath).tasks.named<Jar>("testFixturesJar").get().archiveFile.get().asFile)
        }
    })
    manifest.attributes["Implementation-Title"] = "cangjie-frontend-test-infrastructure"
    manifest.attributes["Implementation-Version"] = project.version.toString()
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        fixtureProjectPaths.map { projectPath ->
            project(projectPath)
                .extensions
                .getByType<JavaPluginExtension>()
                .sourceSets
                .getByName("testFixtures")
                .allSource
        }
    )
}
