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

inline fun Project.sourceSets(crossinline body: SourceSetsBuilder.() -> Unit) = SourceSetsBuilder(this).body()

class SourceSetsBuilder(val project: Project) {
    inline operator fun String.invoke(crossinline body: SourceSet.() -> Unit): SourceSet {
        val sourceSetName = this
        return project.sourceSets.maybeCreate(sourceSetName).apply {
            body()
        }
    }
}

/** 清空源集的所有源目录 */
fun SourceSet.none() {
    java.setSrcDirs(emptyList<String>())
    resources.setSrcDirs(emptyList<String>())
}

/** 添加生成代码目录（默认 gen/） */
fun SourceSet.generatedDir(dirName: String = "gen") {
    java.srcDir(dirName)
}

/** 添加生成测试代码目录（默认 tests-gen/） */
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

val Project.sourceSets: SourceSetContainer
    get() = javaPluginExtension().sourceSets

val Project.mainSourceSet: SourceSet
    get() = javaPluginExtension().mainSourceSet

val Project.testSourceSet: SourceSet
    get() = javaPluginExtension().testSourceSet

val JavaPluginExtension.mainSourceSet: SourceSet
    get() = sourceSets.getByName("main")

val JavaPluginExtension.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")

fun Project.javaPluginExtension(): JavaPluginExtension =
    extensions.getByType(JavaPluginExtension::class.java)
