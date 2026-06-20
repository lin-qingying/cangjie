import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * 可缓存的 Java 主类执行任务。
 *
 * 该任务用于仓库内代码生成器：把生成器 classpath、主类、参数、系统属性、模型输入和输出目录
 * 都纳入 Gradle 指纹，避免普通 JavaExec 无法进入 build cache。
 */
@CacheableTask
abstract class CacheableJavaExec @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    @get:Input
    abstract val mainClass: Property<String>

    @get:Input
    abstract val arguments: ListProperty<String>

    @get:Input
    abstract val systemProperties: MapProperty<String, String>

    @get:InputFiles
    @get:Optional
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedInputs: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Input
    abstract val cleanOutputDirectory: Property<Boolean>

    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    init {
        arguments.convention(emptyList())
        systemProperties.convention(emptyMap())
        cleanOutputDirectory.convention(true)
    }

    @TaskAction
    fun execute() {
        val output = outputDirectory.get().asFile
        if (cleanOutputDirectory.get()) {
            fileSystemOperations.delete {
                delete(output)
            }
        }
        output.mkdirs()

        execOperations.javaexec {
            classpath = this@CacheableJavaExec.classpath
            mainClass.set(this@CacheableJavaExec.mainClass)
            args(this@CacheableJavaExec.arguments.get())
            systemProperties(this@CacheableJavaExec.systemProperties.get())
            workingDir = workingDirectory.get().asFile
        }
    }
}
