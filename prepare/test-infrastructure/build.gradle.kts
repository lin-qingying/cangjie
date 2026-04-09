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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    exclude("META-INF/maven/**")
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
    manifest.attributes["Implementation-Title"] = "cangjie-frontend-test-infrastructure"
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
