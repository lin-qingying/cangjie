import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * 仓库测试任务使用的 JUnit 运行模式。
 */
enum class JUnitMode {
    /**
     * 使用 JUnit 4 runner。
     */
    JUnit4,
    /**
     * 使用 JUnit Platform。
     */
    JUnit5,
}

/**
 * 项目测试约定扩展。
 *
 * 该扩展集中注册普通测试任务、额外测试任务以及测试生成器任务，使各模块的 Gradle 脚本保持一致。
 */
open class ProjectTestsExtension(
    /**
     * 持有该扩展的 Gradle 项目。
     */
    private val project: Project,
) {
    /**
     * 配置默认 `test` 任务。
     *
     * 这是最常用的入口，内部委托到带任务名的重载并固定任务名为 `test`。
     */
    fun testTask(
        parallel: Boolean? = null,
        jUnitMode: JUnitMode,
        body: Test.() -> Unit = {},
    ): TaskProvider<Test> = testTask(
        taskName = "test",
        parallel = parallel,
        jUnitMode = jUnitMode,
        skipInLocalBuild = false,
        body = body,
    ) as TaskProvider<Test>

    /**
     * 注册或配置指定名称的测试任务。
     *
     * 对非默认测试任务会补齐 `test` source set 的 classpath 与 testClassesDirs，并按 [jUnitMode]
     * 配置 JUnit 运行器。
     */
    fun testTask(
        taskName: String,
        parallel: Boolean? = null,
        jUnitMode: JUnitMode,
        skipInLocalBuild: Boolean,
        body: Test.() -> Unit = {},
    ): TaskProvider<out Task> {
        if (skipInLocalBuild) {
            // 当前仓库无 TeamCity 分流需求，保留参数语义但不跳过。
        }

        val testTaskProvider = if (taskName == "test") {
            project.tasks.named(taskName, Test::class.java)
        } else {
            project.tasks.register(taskName, Test::class.java)
        }

        testTaskProvider.configure {
            val sourceSets = project.extensions.getByType<JavaPluginExtension>().sourceSets
            val testSourceSet = sourceSets.getByName("test")
            classpath = project.runtimeClasspathWithoutProjectJars(testSourceSet)
            testClassesDirs = testSourceSet.output.classesDirs

            when (jUnitMode) {
                JUnitMode.JUnit4 -> useJUnit()
                JUnitMode.JUnit5 -> useJUnitPlatform()
            }

            if (parallel != null && jUnitMode == JUnitMode.JUnit4) {
                maxParallelForks = if (parallel) {
                    Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
                } else {
                    1
                }
            }

            doFirst {
                if (jUnitMode != JUnitMode.JUnit4) return@doFirst
                addInnerClassPatternsForExplicitClassIncludes()
            }

            body()
        }

        return testTaskProvider
    }

    /**
     * 注册基于 [CacheableJavaExec] 的测试数据生成任务。
     *
     * 生成器任务会跟踪 testData 输入、tests-gen 输出和生成器 classpath，并自动接入测试编译任务。
     */
    fun testGenerator(
        mainClassName: String,
        generateTestsInBuildDirectory: Boolean = false,
        doNotSetFixturesSourceSetDependency: Boolean = false,
        generatorClasspathSourceSetName: String? = null,
        excludeGeneratorSourceSetOutput: Boolean = false,
        body: CacheableJavaExec.() -> Unit = {},
    ): TaskProvider<CacheableJavaExec> {
        val taskName = "generate" +
            mainClassName.substringAfterLast('.').removeSuffix("Kt") +
            "Tests"

        val sourceSets = project.extensions.getByType<JavaPluginExtension>().sourceSets
        val generatorTask = project.tasks.register<CacheableJavaExec>(taskName) {
            group = "verification"
            description = "Generate tests using $mainClassName"
            mainClass.set(mainClassName)
            workingDirectory.set(project.rootProject.layout.projectDirectory)
            outputDirectory.set(project.layout.projectDirectory.dir("tests-gen"))
            trackedInputs.from(project.layout.projectDirectory.dir("testData"))
            systemProperties.put("line.separator", "\n")
            systemProperties.put("idea.ignore.disabled.plugins", "true")

            /**
             * 生成器任务的类路径需要与“生成器类真正声明在哪个 source set”保持一致。
             *
             * 默认策略仍然兼容旧模块：
             * 1. 明确指定 `generatorClasspathSourceSetName` 时，严格使用该 source set；
             * 2. 旧参数 `doNotSetFixturesSourceSetDependency` 为真时，退回 `test`；
             * 3. 否则优先使用当前模块的 `testFixtures`，再退回 `test`。
             *
             * 这样可以支持 Kotlin analysis 那种“三层结构”：
             * - testData 在 API 模块
             * - 抽象用例在 impl-base 的 testFixtures
             * - backend runner 在当前模块的 test source set
             */
            val classpathSourceSet = when {
                generatorClasspathSourceSetName != null -> sourceSets.getByName(generatorClasspathSourceSetName)
                doNotSetFixturesSourceSetDependency -> sourceSets.getByName("test")
                sourceSets.findByName("testFixtures") != null -> sourceSets.getByName("testFixtures")
                else -> sourceSets.getByName("test")
            }
            /**
             * 某些 generated-tests 场景下，生成器类来自“外部依赖模块的 testFixtures”，
             * 而当前模块只提供 tests-gen 输出目录。
             *
             * 这时若把当前 source set 的 output 放回 generator classpath，会形成：
             * `compileTestKotlin -> generateTests -> test runtimeClasspath -> testClasses`
             * 的环。这里提供显式开关，让生成器只依赖外部运行时依赖，而不依赖当前模块测试产物。
             */
            val generatorClasspath: FileCollection = if (excludeGeneratorSourceSetOutput) {
                /**
                 * 这里不能继续使用 `SourceSet.runtimeClasspath.minus(output)`。
                 * 尽管文件集合被减掉了 output，本身仍会保留对当前 source set 输出任务的 build 依赖，
                 * 进而形成 `compileTestKotlin -> generateTests -> testClasses` 的环。
                 *
                 * 因此这里显式退回到底层 runtimeClasspath configuration，只保留“依赖产物”本身。
                 */
                val dependencyRuntimeClasspath: Configuration =
                    project.configurations.getByName(classpathSourceSet.runtimeClasspathConfigurationName)
                project.dependencyRuntimeClasspathWithoutProjectJars(dependencyRuntimeClasspath)
            } else {
                project.runtimeClasspathWithoutProjectJars(classpathSourceSet)
            }
            classpath.from(generatorClasspath)

            if (!generateTestsInBuildDirectory) {
                /**
                 * Kotlin 的 test generator 任务默认依赖 workingDir = rootDir 解析测试数据相对路径，
                 * 这里不再向 main 传入仓库根目录参数，避免把无语义参数混入 generator CLI。
                 */
            }
            body()
        }

        project.tasks.matching { it.name == "compileTestKotlin" || it.name == "compileTestJava" }
            .configureEach {
                dependsOn(generatorTask)
            }

        return generatorTask
    }
}

/**
 * 为显式指定外部测试类的 Gradle include pattern 自动补充内部类匹配。
 *
 * Gradle 命令行 `--tests SomeGeneratedClass` 默认不会包含内部类，这会漏跑生成式测试套件；
 * 该函数在 JUnit4 测试执行前补充 `SomeGeneratedClass$*` 形式的模式。
 */
private fun Test.addInnerClassPatternsForExplicitClassIncludes() {
    val defaultFilterClass = "org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter"
    val filterObject = filter
    if (!Class.forName(defaultFilterClass).isInstance(filterObject)) return

    val commandLinePatterns = runCatching {
        val method = filterObject.javaClass.getMethod("getCommandLineIncludePatterns")
        @Suppress("UNCHECKED_CAST")
        (method.invoke(filterObject) as Set<String>).toMutableSet()
    }.getOrDefault(mutableSetOf())

    val patterns = filter.includePatterns + commandLinePatterns
    if (patterns.isEmpty() || patterns.any { '*' in it }) return

    patterns.forEach { pattern ->
        val lastPart = pattern.substringAfterLast('.')
        if (lastPart.firstOrNull()?.isUpperCase() != true) return@forEach

        val innerClassPattern = "${pattern}\\$*"
        if (pattern in commandLinePatterns) {
            commandLinePatterns.add(innerClassPattern)
            runCatching {
                val setter = filterObject.javaClass.getMethod("setCommandLineIncludePatterns", Set::class.java)
                setter.invoke(filterObject, commandLinePatterns)
            }
        } else {
            filter.includePatterns.add(innerClassPattern)
        }
    }
}
