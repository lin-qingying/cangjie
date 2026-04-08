import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    `java-library`
}

description = "仓颉前端 analysis 测试框架公开门面工件。"

val fixtureProjectPaths = listOf(
    ":analysis:analysis-test-framework",
    ":tests:test-infrastructure",
)

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    dependsOn(fixtureProjectPaths.map { "$it:testFixturesJar" })
    from({
        fixtureProjectPaths.flatMap { projectPath ->
            val fixtureSourceSet = project(projectPath)
                .extensions
                .getByType<JavaPluginExtension>()
                .sourceSets
                .getByName("testFixtures")

            fixtureSourceSet.runtimeClasspath.files.mapNotNull { file ->
                when {
                    !file.exists() -> null
                    file.isDirectory -> fileTree(file)
                    else -> zipTree(file)
                }
            }
        }
    })
    manifest.attributes["Implementation-Title"] = "cangjie-frontend-analysis-test-framework"
    manifest.attributes["Implementation-Version"] = project.version.toString()
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(fixtureProjectPaths.map { "$it:testFixturesJar" })
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
