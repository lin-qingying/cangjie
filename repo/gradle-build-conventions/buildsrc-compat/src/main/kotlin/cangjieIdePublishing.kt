import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

/**
 * 对齐 Kotlin 的 publishJarsForIde 模式。
 *
 * 将一组内部子模块打包为单个 fat jar 发布给 IDE 插件仓库。
 * 直接引用各项目 jar 输出，不通过 configuration resolution，避免传递依赖泄露。
 *
 * @param projects             需要打包的内部模块路径列表
 * @param libraryDependencies  需要一并打入 fat jar 的三方库坐标（可选，暂未使用）
 * @param apiDependencies      需要透传给消费方的三方库坐标，写入 POM <dependencies>（compile scope）
 *
 * @see <a href="https://github.com/JetBrains/kotlin">Kotlin repoArtifacts.kt publishJarsForIde</a>
 */
fun Project.publishCangjieJarsForIde(
    projects: List<String>,
    libraryDependencies: List<String> = emptyList(),
    apiDependencies: List<String> = emptyList(),       // ← 新增参数
) {
    pluginManager.apply("java-library")

    tasks.named<Jar>("jar") {
        projects.forEach { dependsOn("$it:jar") }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/maven/**")
        from({
            projects.map { projectPath ->
                project.zipTree(
                    project(projectPath).tasks.named<Jar>("jar").get().archiveFile.get().asFile
                )
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

    // 将 apiDependencies 加入 api configuration：
    // - 编译期：本模块可见（java-library 插件保证）
    // - 发布期：cangjie-publishing 通过 components["java"] 自动将 api 依赖
    //           以 compile scope 写入 POM，消费方 Gradle/Maven 均可自动继承
    if (apiDependencies.isNotEmpty()) {
        dependencies {
            apiDependencies.forEach { coord ->
                add("api", coord)
            }
        }
    }
}