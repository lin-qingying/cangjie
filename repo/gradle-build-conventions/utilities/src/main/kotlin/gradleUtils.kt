import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import java.util.concurrent.Callable

/**
 * 获取或创建指定名称的 configuration。
 */
fun ConfigurationContainer.getOrCreate(name: String): Configuration = findByName(name) ?: create(name)

/**
 * 向指定 configuration 添加依赖，并可选配置 module dependency。
 */
fun DependencyHandler.add(configurationName: String, dependencyNotation: Any, configure: (ModuleDependency.() -> Unit)?) {
    if (configure != null) {
        add(configurationName, dependencyNotation, closureOf(configure))
    } else {
        add(configurationName, dependencyNotation)
    }
}

/**
 * 将任务产物发布到指定 configuration。
 */
fun <T : Task> Project.addArtifact(
    configurationName: String,
    task: TaskProvider<T>,
    body: ConfigurablePublishArtifact.() -> Unit = {},
): PublishArtifact {
    configurations.maybeCreate(configurationName)
    return artifacts.add(configurationName, task, body)
}

/**
 * 延迟计算 copy source，避免配置阶段提前解析文件集合。
 */
inline fun CopySourceSpec.from(crossinline filesProvider: () -> Any?): CopySourceSpec = from(Callable { filesProvider() })

/**
 * 查找当前 project 的 Java plugin extension。
 */
fun Project.findJavaPluginExtension(): JavaPluginExtension? = extensions.findByType()
