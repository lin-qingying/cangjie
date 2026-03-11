import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

enum class JUnitMode {
    JUnit4,
    JUnit5,
}

open class ProjectTestsExtension(
    private val project: Project,
) {
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
            if (taskName != "test" && classpath.isEmpty) {
                classpath = sourceSets.getByName("test").runtimeClasspath
                testClassesDirs = sourceSets.getByName("test").output.classesDirs
            }

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

    fun testGenerator(
        mainClassName: String,
        generateTestsInBuildDirectory: Boolean = false,
        doNotSetFixturesSourceSetDependency: Boolean = false,
        body: JavaExec.() -> Unit = {},
    ): TaskProvider<JavaExec> {
        val taskName = "generate" +
            mainClassName.substringAfterLast('.').removeSuffix("Kt") +
            "Tests"

        val sourceSets = project.extensions.getByType<JavaPluginExtension>().sourceSets
        val generatorTask = project.tasks.register<JavaExec>(taskName) {
            group = "verification"
            description = "Generate tests using $mainClassName"
            mainClass.set(mainClassName)

            val classpathSourceSet = when {
                doNotSetFixturesSourceSetDependency -> sourceSets.getByName("test")
                sourceSets.findByName("testFixtures") != null -> sourceSets.getByName("testFixtures")
                else -> sourceSets.getByName("test")
            }
            classpath = classpathSourceSet.runtimeClasspath

            if (!generateTestsInBuildDirectory) {
                args(project.rootDir.absolutePath)
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

