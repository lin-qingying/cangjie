import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

/**
 * 对齐 Kotlin 的 publishJarsForIde 模式。
 *
 * 将一组内部子模块打包为单个 fat jar 发布给 IDE 插件仓库。
 * 直接引用各项目 jar 输出，不通过 configuration resolution，避免传递依赖泄露。
 *
 * @param projects      需要打包的一方模块路径列表
 * @param libraryDependencies 需要一并打入的三方库坐标（可选，暂未使用）
 *
 * @see <a href="https://github.com/JetBrains/kotlin">Kotlin repoArtifacts.kt publishJarsForIde</a>
 */
fun Project.publishCangjieJarsForIde(
    projects: List<String>,
    libraryDependencies: List<String> = emptyList(),
) {
    pluginManager.apply("java-library")

    tasks.named<Jar>("jar") {
        projects.forEach { dependsOn("$it:jar") }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/maven/**")
        from({
            projects.map { projectPath ->
                project.zipTree(project(projectPath).tasks.named<Jar>("jar").get().archiveFile.get().asFile)
            }
        })
    }

    val javaExtension = extensions.getByType<JavaPluginExtension>()
    javaExtension.withSourcesJar()
    javaExtension.withJavadocJar()

    tasks.named<Jar>("sourcesJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        for (projectPath in projects) {
            val projectTasks = project(projectPath).tasks
            if (projectTasks.names.any { it == "compileKotlin" }) {
                dependsOn(projectTasks.named("compileKotlin").map { it.dependsOn })
            }
        }
        from({
            projects
                .map(::project)
                .filter { it.plugins.hasPlugin("java-base") }
                .map { it.extensions.getByType<JavaPluginExtension>().sourceSets.getByName("main").allSource }
        })
    }
}
