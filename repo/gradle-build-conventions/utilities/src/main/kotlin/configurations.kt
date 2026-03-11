import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

fun Project.getOrCreateConfiguration(name: String, body: Configuration.() -> Unit = {}): Configuration {
    return configurations.findByName(name)?.apply { body() } ?: configurations.create(name) { body() }
}
