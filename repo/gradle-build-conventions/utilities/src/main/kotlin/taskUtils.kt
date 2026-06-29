import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * 获取已存在的任务，或在不存在时创建同名任务。
 *
 * 该工具用于构建约定插件在多处配置同一任务时保持幂等，避免重复注册任务导致 Gradle 配置失败。
 */
inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, noinline body: T.() -> Unit): TaskProvider<T> =
    if (tasks.names.contains(taskName)) tasks.named(taskName, T::class.java).apply { configure(body) }
    else tasks.register(taskName, T::class.java, body)
