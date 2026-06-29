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
    /**
     * Gradle 注入的进程执行服务，用于以受 Gradle 管理的方式启动 Java 主类。
     */
    private val execOperations: ExecOperations,
    /**
     * Gradle 注入的文件系统服务，用于在任务执行前清理可缓存输出目录。
     */
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    /**
     * 生成器运行所需的类路径。
     *
     * 该输入按 classpath 语义参与任务指纹，保证依赖 jar 或测试 fixture 变化时重新执行生成器。
     */
    @get:Classpath
    abstract val classpath: ConfigurableFileCollection

    /**
     * 要执行的 Java/Kotlin 主类名。
     *
     * 通常指向仓库内测试生成器或源码生成器的 `main` 入口。
     */
    @get:Input
    abstract val mainClass: Property<String>

    /**
     * 传递给生成器主类的命令行参数。
     */
    @get:Input
    abstract val arguments: ListProperty<String>

    /**
     * 传递给生成器 JVM 进程的系统属性。
     */
    @get:Input
    abstract val systemProperties: MapProperty<String, String>

    /**
     * 生成器需要纳入增量判断的模型输入文件集合。
     *
     * 例如 testData、模板文件或生成器读取的声明清单；为空时允许任务继续执行。
     */
    @get:InputFiles
    @get:Optional
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trackedInputs: ConfigurableFileCollection

    /**
     * 生成器产物目录。
     *
     * 该目录作为 Gradle 输出目录参与 up-to-date 与 build cache 判断。
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * 是否在执行生成器前清空 [outputDirectory]。
     *
     * 默认清空，避免旧生成文件在输入收缩后残留。
     */
    @get:Input
    abstract val cleanOutputDirectory: Property<Boolean>

    /**
     * 生成器进程的工作目录。
     *
     * 工作目录只影响进程定位相对路径，不作为任务输入参与缓存指纹。
     */
    @get:Internal
    abstract val workingDirectory: DirectoryProperty

    init {
        arguments.convention(emptyList())
        systemProperties.convention(emptyMap())
        cleanOutputDirectory.convention(true)
    }

    /**
     * 执行生成器主类并维护输出目录生命周期。
     */
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
