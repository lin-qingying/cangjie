import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer

/**
 * SourceSet 工具（对齐 Kotlin 的 sourceSets.kt）。
 *
 * 提供 DSL 扩展用于在 build.gradle.kts 中简洁配置源集：
 * ```
 * sourceSets {
 *     "main" { none() }
 *     "test" { none() }
 *     "testFixtures" { projectDefault() }
 * }
 * ```
 */

/**
 * 配置当前项目的 Gradle SourceSet。
 *
 * 该入口通过 [SourceSetsBuilder] 提供 `"main" { ... }` 形式的简洁 DSL。
 */
inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) = SourceSetsBuilder(this).body()

/**
 * SourceSet DSL 的构建器。
 *
 * 构建器持有目标 [project]，并把字符串调用语法映射为 source set 的创建与配置。
 */
class SourceSetsBuilder(
    /**
     * 正在配置 source set 的 Gradle 项目。
     */
    val project: Project,
) {
    /**
     * 获取或创建当前字符串命名的 source set，并在其上执行 [body]。
     */
    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit): SourceSet {
        val sourceSetName = this
        return project.sourceSets.maybeCreate(sourceSetName).apply {
            body()
        }
    }
}

/**
 * 清空当前 source set 的源码目录和资源目录。
 */
fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

/**
 * 向当前 source set 添加生成源码目录。
 */
fun SourceSet.generatedDir(dirName: String = "gen") {
    java.srcDir(dirName)
}

/**
 * 向当前 source set 添加生成测试源码目录。
 */
fun SourceSet.generatedTestDir(dirName: String = "tests-gen") {
    java.srcDir(dirName)
}

/**
 * 设置为项目默认源目录布局（对齐 Kotlin 风格）：
 * - main → src/
 * - test → test/, tests/
 * - testFixtures → testFixtures/
 *
 * 注意：使用 srcDirs()（追加）而非 setSrcDirs()（替换），
 * 避免破坏 java-test-fixtures 插件和 Kotlin Gradle Plugin
 * 注册的输出目录链。
 */
fun SourceSet.projectDefault() {
    when (name) {
        "main" -> {
            java.setSrcDirs(listOf("src"))
            resources.setSrcDirs(listOf("resources"))
        }
        "test" -> {
            java.setSrcDirs(listOf("test", "tests"))
            resources.setSrcDirs(listOf("testResources"))
        }
        "testFixtures" -> {
            java.srcDirs("testFixtures")
            resources.srcDir("testFixturesResources")
        }
        else -> error("Unknown source set $name")
    }
}

/**
 * 当前项目的 Gradle source set 容器。
 */
val Project.sourceSets: SourceSetContainer
    get() = javaPluginExtension().sourceSets

/**
 * 当前项目的 `main` source set。
 */
val Project.mainSourceSet: SourceSet
    get() = javaPluginExtension().mainSourceSet

/**
 * 当前项目的 `test` source set。
 */
val Project.testSourceSet: SourceSet
    get() = javaPluginExtension().testSourceSet

/**
 * Java 插件扩展中的 `main` source set。
 */
val JavaPluginExtension.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

/**
 * Java 插件扩展中的 `test` source set。
 */
val JavaPluginExtension.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")

/**
 * 获取当前项目的 Java 插件扩展。
 */
fun Project.javaPluginExtension(): JavaPluginExtension =
    extensions.getByType(JavaPluginExtension::class.java)
