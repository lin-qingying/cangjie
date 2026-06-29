package org.cangnova.buildtools.nativecompile

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * JNI 原生编译的 Gradle 扩展配置。
 *
 * 构建脚本可通过 `nativeCompile { ... }` 配置 C/C++ 源码目录、头文件目录、LLVM 位置、
 * 编译参数、链接参数以及最终共享库名称。
 */
open class NativeCompileExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * 参与原生编译的源码目录集合。
     */
    val sourceDirs: ConfigurableFileCollection = objects.fileCollection()
    /**
     * 额外传递给编译器的头文件目录集合。
     */
    val includeDirs: ConfigurableFileCollection = objects.fileCollection()
    /**
     * 追加到每个源码编译命令的编译器参数。
     */
    val compilerArgs: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    /**
     * 追加到最终共享库链接命令的链接器参数。
     */
    val linkerArgs: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    /**
     * 以 `-D` 形式传递给编译器的宏定义集合。
     */
    val definitions: SetProperty<String> = objects.setProperty(String::class.java).convention(emptySet())
    /**
     * 生成的共享库基础名称，不包含平台前缀和扩展名。
     */
    val outputName: Property<String> = objects.property(String::class.java).convention("cangjie_llvm_jni")
    /**
     * LLVM 安装根目录。
     *
     * 未显式配置时会从 Gradle 属性、环境变量、llvm-config 和平台默认路径中探测。
     */
    val llvmDir: Property<String> = objects.property(String::class.java)
    /**
     * LLVM 不存在时是否跳过原生编译任务。
     */
    val skipWhenLlvmMissing: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * 向 [sourceDirs] 添加一个源码目录。
     */
    fun sourceDir(path: Any) {
        sourceDirs.from(path)
    }

    /**
     * 向 [includeDirs] 添加一个头文件目录。
     */
    fun includeDir(path: Any) {
        includeDirs.from(path)
    }
}

/**
 * 编译 JNI 原生源码并链接为平台共享库的 Gradle 任务。
 */
abstract class NativeCompileTask @Inject constructor(
    /**
     * Gradle 注入的进程执行服务，用于调用 C/C++ 编译器和链接器。
     */
    private val execOperations: ExecOperations,
) : DefaultTask() {
    /**
     * 待扫描 C/C++ 源码的目录集合。
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirs: ConfigurableFileCollection

    /**
     * 参与任务输入指纹的头文件目录集合。
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeDirs: ConfigurableFileCollection

    /**
     * 传递给每个编译动作的额外编译器参数。
     */
    @get:Input
    abstract val compilerArgs: ListProperty<String>

    /**
     * 传递给最终链接动作的额外链接器参数。
     */
    @get:Input
    abstract val linkerArgs: ListProperty<String>

    /**
     * 参与编译的宏定义集合。
     */
    @get:Input
    abstract val definitions: SetProperty<String>

    /**
     * 输出共享库的基础名称。
     */
    @get:Input
    abstract val outputName: Property<String>

    /**
     * LLVM 不可用时是否跳过任务。
     */
    @get:Input
    abstract val skipWhenLlvmMissing: Property<Boolean>

    /**
     * 显式配置的 LLVM 安装根目录。
     */
    @get:Input
    @get:Optional
    abstract val llvmDir: Property<String>

    /**
     * 任务产出的平台共享库文件。
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * 扫描源码、解析工具链并执行编译与链接。
     */
    @TaskAction
    fun compile() {
        val platform = NativePlatform.current()
        val sources = sourceDirs.asFileTree
            .matching { include("**/*.c", "**/*.cc", "**/*.cpp") }
            .files
            .sortedBy { it.absolutePath }

        if (sources.isEmpty()) {
            logger.lifecycle("No native sources found, skipping native compile task.")
            return
        }

        val llvmRoot = LlvmLocator.locate(llvmDir.orNull, platform)
        if (llvmRoot == null) {
            if (skipWhenLlvmMissing.get()) {
                logger.warn("LLVM development libraries not found, skipping native build")
                return
            }
            throw GradleException("LLVM development libraries not found")
        }

        val compiler = ToolchainResolver.resolveCompiler(llvmRoot, platform)
        val sharedLinkFlags = ToolchainResolver.resolveSharedLinkFlags(compiler, platform)
        val llvmInclude = File(llvmRoot, "include")
        val includeArgs = buildList {
            add("-I${llvmInclude.absolutePath}")
            includeDirs.files.sortedBy { it.absolutePath }.forEach { add("-I${it.absolutePath}") }
            JniHeaders.current().forEach { add("-I${it.absolutePath}") }
        }

        val objectDir = temporaryDir.resolve("obj").also { it.mkdirs() }
        val objects = mutableListOf<File>()
        val compileBaseArgs = buildList {
            addAll(platform.compileFlags)
            addAll(definitions.get().sorted().map { "-D$it" })
            add("-D${platform.macroName}=1")
            addAll(compilerArgs.get())
            addAll(includeArgs)
        }

        sources.forEach { source ->
            val objectFile = objectDir.resolve(source.nameWithoutExtension + platform.objectSuffix)
            execOperations.exec {
                executable = compiler
                args(*(compileBaseArgs + listOf("-c", source.absolutePath, "-o", objectFile.absolutePath)).toTypedArray())
            }
            objects += objectFile
        }

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()

        val llvmLibDirs = listOf(File(llvmRoot, "lib"), File(llvmRoot, "lib64"))
            .filter { it.exists() && it.isDirectory }
            .map { "-L${it.absolutePath}" }
        val llvmStaticLibs = if (sharedLinkFlags.contains("-shared")) {
            File(llvmRoot, "lib")
                .listFiles()
                ?.asSequence()
                ?.filter { it.isFile && it.name.startsWith("libLLVM") && it.name.endsWith(".a") }
                ?.sortedBy { it.name }
                ?.map { it.absolutePath }
                ?.toList()
                .orEmpty()
        } else {
            emptyList()
        }

        execOperations.exec {
            executable = compiler
            args(
                *(buildList {
                    addAll(sharedLinkFlags)
                    addAll(linkerArgs.get())
                    addAll(llvmLibDirs)
                    addAll(objects.map { it.absolutePath })
                    if (llvmStaticLibs.isNotEmpty()) {
                        add("-Wl,--start-group")
                        addAll(llvmStaticLibs)
                        add("-Wl,--end-group")
                        // LLVM static archives on MinGW require explicit system libs at the end.
                        addAll(listOf("-lwinpthread", "-lole32", "-luuid", "-lpsapi"))
                    }
                    add("-o")
                    add(output.absolutePath)
                }.toTypedArray()),
            )
        }
    }
}

/**
 * 注册 JNI 原生编译扩展和 `nativeCompile` 任务的 Gradle 插件。
 */
class NativeCompilePlugin : Plugin<Project> {
    /**
     * 将原生编译扩展和任务挂载到目标 [project]。
     */
    override fun apply(project: Project) {
        val platform = NativePlatform.current()
        val extension = project.extensions.create("nativeCompile", NativeCompileExtension::class.java)
        extension.sourceDir("src/main/native")
        extension.includeDir("src/main/include")
        extension.definitions.convention(setOf(platform.macroName))
        extension.llvmDir.convention(project.providers.gradleProperty("llvm.dir"))

        project.tasks.register("nativeCompile", NativeCompileTask::class.java) {
            group = "build"
            description = "Compile JNI native sources to a shared library."
            sourceDirs.from(extension.sourceDirs)
            includeDirs.from(extension.includeDirs)
            compilerArgs.set(extension.compilerArgs)
            linkerArgs.set(extension.linkerArgs)
            definitions.set(extension.definitions)
            outputName.set(extension.outputName)
            skipWhenLlvmMissing.set(extension.skipWhenLlvmMissing)
            llvmDir.set(extension.llvmDir)
            outputFile.set(
                project.layout.buildDirectory.file(
                    extension.outputName.map { "native/${platform.sharedLibraryFileName(it)}" },
                ),
            )
        }
    }
}

/**
 * 当前宿主平台的原生编译参数集合。
 */
private data class NativePlatform(
    /**
     * 规范化后的操作系统名称。
     */
    val os: String,
    /**
     * 规范化后的 CPU 架构名称。
     */
    val arch: String,
    /**
     * 中间目标文件扩展名。
     */
    val objectSuffix: String,
    /**
     * 注入给原生源码的平台宏名称。
     */
    val macroName: String,
    /**
     * 当前平台编译共享库对象文件所需的基础编译参数。
     */
    val compileFlags: List<String>,
    /**
     * 当前平台链接共享库所需的基础链接参数。
     */
    val sharedLinkFlags: List<String>,
    /**
     * 当前平台共享库扩展名。
     */
    val sharedLibraryExtension: String,
) {
    /**
     * 根据平台命名规则生成共享库文件名。
     */
    fun sharedLibraryFileName(baseName: String): String {
        return if (os == "windows") "$baseName.$sharedLibraryExtension" else "lib$baseName.$sharedLibraryExtension"
    }

    /**
     * 宿主平台探测入口。
     */
    companion object {
        /**
         * 根据 JVM 暴露的 `os.name` 和 `os.arch` 构造当前平台描述。
         */
        fun current(): NativePlatform {
            val osName = System.getProperty("os.name").lowercase()
            val archName = System.getProperty("os.arch").lowercase()
            val arch = when (archName) {
                "amd64", "x86_64" -> "x86_64"
                "aarch64", "arm64" -> "aarch64"
                else -> archName
            }
            return when {
                osName.contains("win") -> NativePlatform(
                    os = "windows",
                    arch = arch,
                    objectSuffix = ".obj",
                    macroName = "CANGJIE_WINDOWS",
                    compileFlags = emptyList(),
                    sharedLinkFlags = listOf("/DLL"),
                    sharedLibraryExtension = "dll",
                )
                osName.contains("mac") -> NativePlatform(
                    os = "macos",
                    arch = arch,
                    objectSuffix = ".o",
                    macroName = "CANGJIE_MACOS",
                    compileFlags = listOf("-fPIC"),
                    sharedLinkFlags = listOf("-dynamiclib"),
                    sharedLibraryExtension = "dylib",
                )
                osName.contains("linux") -> NativePlatform(
                    os = "linux",
                    arch = arch,
                    objectSuffix = ".o",
                    macroName = "CANGJIE_LINUX",
                    compileFlags = listOf("-fPIC"),
                    sharedLinkFlags = listOf("-shared"),
                    sharedLibraryExtension = "so",
                )
                else -> throw GradleException("Unsupported host platform: $osName-$archName")
            }
        }
    }
}

/**
 * LLVM 安装目录定位器。
 */
private object LlvmLocator {
    /**
     * 按显式配置、环境变量、llvm-config 和平台默认路径顺序查找可用 LLVM 根目录。
     */
    fun locate(configuredLlvmDir: String?, platform: NativePlatform): File? {
        val candidates = mutableListOf<String>()
        if (!configuredLlvmDir.isNullOrBlank()) candidates += configuredLlvmDir
        System.getenv("LLVM_DIR")?.takeIf { it.isNotBlank() }?.let { candidates += it }
        llvmConfigPrefix()?.let { candidates += it }
        when (platform.os) {
            "linux" -> candidates += "/usr/lib/llvm-18"
            "macos" -> candidates += "/opt/homebrew/opt/llvm@18"
            "windows" -> candidates += "C:/Program Files/LLVM"
        }
        return candidates
            .asSequence()
            .map { File(it) }
            .firstOrNull(::isValidLlvmDir)
    }

    /**
     * 通过 `llvm-config --prefix` 获取 LLVM 安装前缀。
     */
    private fun llvmConfigPrefix(): String? {
        return runCatching {
            val process = ProcessBuilder("llvm-config", "--prefix")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        }.getOrNull()
    }

    /**
     * 判断目录是否包含 LLVM C API 头文件以及可链接库目录。
     */
    private fun isValidLlvmDir(dir: File): Boolean {
        if (!dir.exists()) return false
        val include = File(dir, "include/llvm-c/Core.h")
        val lib = File(dir, "lib")
        val lib64 = File(dir, "lib64")
        return include.exists() && (lib.exists() || lib64.exists())
    }
}

/**
 * C/C++ 编译器与平台链接参数解析器。
 */
private object ToolchainResolver {
    /**
     * 解析当前平台可用的 C++ 编译器路径。
     */
    fun resolveCompiler(llvmRoot: File, platform: NativePlatform): String {
        val executable = if (platform.os == "windows") "clang++.exe" else "clang++"
        val llvmClang = File(llvmRoot, "bin/$executable")
        if (llvmClang.exists()) return llvmClang.absolutePath

        val ordered = if (platform.os == "windows") {
            listOf("clang++.exe", "clang++", "g++.exe", "g++")
        } else {
            listOf("clang++", "g++")
        }
        return ordered.firstOrNull { existsOnPath(it) }
            ?: throw GradleException("No suitable C++ compiler found (tried: ${ordered.joinToString()})")
    }

    /**
     * 判断指定可执行文件是否存在于 PATH 中。
     */
    private fun existsOnPath(executable: String): Boolean {
        val cmd = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("where", executable)
        } else {
            listOf("which", executable)
        }
        return runCatching {
            ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
                .waitFor() == 0
        }.getOrDefault(false)
    }

    /**
     * 根据编译器类型解析共享库链接参数。
     */
    fun resolveSharedLinkFlags(compiler: String, platform: NativePlatform): List<String> {
        if (platform.os != "windows") return platform.sharedLinkFlags
        val name = File(compiler).name.lowercase()
        return when {
            name == "cl.exe" || name == "clang-cl.exe" -> listOf("/DLL")
            else -> listOf("-shared")
        }
    }
}

/**
 * 当前 JDK 的 JNI 头文件定位器。
 */
private object JniHeaders {
    /**
     * 返回当前 JDK 可用的 JNI 公共头文件目录和平台头文件目录。
     */
    fun current(): List<File> {
        val javaHome = File(System.getProperty("java.home")).canonicalFile
        val home = if (File(javaHome, "include").exists()) javaHome else javaHome.parentFile
        val includeDir = File(home, "include")
        if (!includeDir.exists()) return emptyList()
        val osSubDir = when {
            System.getProperty("os.name").lowercase().contains("win") -> "win32"
            System.getProperty("os.name").lowercase().contains("mac") -> "darwin"
            else -> "linux"
        }
        return listOf(includeDir, File(includeDir, osSubDir)).filter { it.exists() }
    }
}
