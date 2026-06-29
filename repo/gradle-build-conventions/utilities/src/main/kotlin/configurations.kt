import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * 获取或创建指定名称的 Gradle configuration，并应用配置块。
 */
fun Project.getOrCreateConfiguration(name: String, body: Configuration.() -> Unit = {}): Configuration {
    return configurations.findByName(name)?.apply { body() } ?: configurations.create(name) { body() }
}
