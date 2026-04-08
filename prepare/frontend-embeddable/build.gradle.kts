import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

plugins {
    `java-library`
    id("com.gradleup.shadow")
}

description = "仓颉前端 embeddable shaded/relocated 公开工件。"

val bundledProjectPaths = listOf(
    ":compiler:frontend",
    ":dependencies:intellij-core",
)

dependencies {
    bundledProjectPaths.forEach { projectPath ->
        implementation(project(projectPath))
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true

    // 屏蔽签名文件与原始注解包，避免合并后出现签名失效与重复资源。
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("org/jetbrains/annotations/**")

    mergeServiceFiles()
    configureCangjieEmbeddableRelocation()

    manifest.attributes["Implementation-Title"] = "cangjie-frontend-embeddable"
    manifest.attributes["Implementation-Version"] = project.version.toString()
}

tasks.named<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isZip64 = true
    dependsOn(bundledProjectPaths.map { "$it:sourcesJar" })
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
