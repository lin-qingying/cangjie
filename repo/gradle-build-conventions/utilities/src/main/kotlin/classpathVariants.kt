import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet

/**
 * 为一方模块发布 classes/resources secondary variants。
 *
 * Gradle 默认 runtimeElements 已有 classes/resources，但 apiElements 仍只暴露 jar。Kotlin/Java 编译
 * classpath 请求 `java-api + classes` 时，如果生产端没有 apiElements/classes，就会退回 apiElements jar。
 * 这里按 source set 补齐 classes variant，使编译和测试夹具依赖都能直接消费类目录。
 */
fun Project.exposeClassesVariantsForProjectDependencyResolution() {
    plugins.withId("java") {
        val sourceSets = extensions.getByType(JavaPluginExtension::class.java).sourceSets
        fun configureSourceSetVariants(sourceSet: SourceSet) {
            configurations
                .matching {
                    it.name == sourceSet.apiElementsConfigurationName ||
                        it.name == sourceSet.runtimeElementsConfigurationName
                }
                .configureEach {
                    outgoing.variants.maybeCreate("classes").apply {
                        attributes {
                            attribute(
                                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(LibraryElements::class.java, LibraryElements.CLASSES),
                            )
                        }
                        sourceSet.output.classesDirs.files.forEach { classesDir ->
                            if (!artifacts.files.contains(classesDir)) {
                                artifact(classesDir) {
                                    type = ArtifactTypeDefinition.JVM_CLASS_DIRECTORY
                                    builtBy(tasks.named(sourceSet.classesTaskName))
                                }
                            }
                        }
                    }
                }

            configurations
                .matching { it.name == sourceSet.runtimeElementsConfigurationName }
                .configureEach {
                    val resourcesDir = sourceSet.output.resourcesDir ?: return@configureEach
                    outgoing.variants.maybeCreate("resources").apply {
                        attributes {
                            attribute(
                                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                                objects.named(LibraryElements::class.java, LibraryElements.RESOURCES),
                            )
                        }
                        if (!artifacts.files.contains(resourcesDir)) {
                            artifact(resourcesDir) {
                                type = ArtifactTypeDefinition.JVM_RESOURCES_DIRECTORY
                                builtBy(tasks.named(sourceSet.processResourcesTaskName))
                            }
                        }
                    }
                }
        }

        sourceSets.forEach { sourceSet: SourceSet ->
            configureSourceSetVariants(sourceSet)
        }
        plugins.withId("java-test-fixtures") {
            sourceSets.findByName("testFixtures")?.let(::configureSourceSetVariants)
        }
    }
}

/**
 * 构造运行时 classpath，并让一方 project dependency 使用 classes/resources secondary variants。
 *
 * Gradle 默认会把 project dependency 的 java-runtime 变体解析为 jar。对测试与仓库内代码生成而言，
 * 直接使用上游类目录和资源目录即可满足运行语义，同时可以避免启动前无意义地打包大量 jar。
 */
fun Project.runtimeClasspathWithoutProjectJars(sourceSet: SourceSet): FileCollection {
    val sourceSets = extensions.getByType(JavaPluginExtension::class.java).sourceSets
    val runtimeClasspath = configurations.getByName(sourceSet.runtimeClasspathConfigurationName)
    val ownOutputs = if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) {
        files(sourceSet.output)
    } else {
        files(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output, sourceSet.output)
    }

    return ownOutputs + dependencyRuntimeClasspathWithoutProjectJars(runtimeClasspath)
}

/**
 * 只转换 configuration 中的依赖产物，不加入当前 source set 自身输出。
 *
 * 该函数用于 generator classpath 等“只需要依赖运行时”的场景，避免重新引入当前 source set 输出任务依赖。
 */
fun Project.dependencyRuntimeClasspathWithoutProjectJars(runtimeClasspath: Configuration): FileCollection {
    val projectClasses = runtimeClasspath.incoming.artifactView {
        componentFilter { it is ProjectComponentIdentifier }
        attributes {
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements::class.java, LibraryElements.CLASSES),
            )
        }
    }.files
    val projectResources = runtimeClasspath.incoming.artifactView {
        componentFilter { it is ProjectComponentIdentifier }
        attributes {
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements::class.java, LibraryElements.RESOURCES),
            )
        }
    }.files
    val externalArtifacts = runtimeClasspath.incoming.artifactView {
        componentFilter { it !is ProjectComponentIdentifier }
    }.files

    return files(projectClasses, projectResources, externalArtifacts)
}
