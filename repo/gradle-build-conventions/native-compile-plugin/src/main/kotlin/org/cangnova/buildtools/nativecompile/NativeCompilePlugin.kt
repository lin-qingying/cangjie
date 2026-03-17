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

open class NativeCompileExtension @Inject constructor(objects: ObjectFactory) {
    val sourceDirs: ConfigurableFileCollection = objects.fileCollection()
    val includeDirs: ConfigurableFileCollection = objects.fileCollection()
    val compilerArgs: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    val linkerArgs: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    val definitions: SetProperty<String> = objects.setProperty(String::class.java).convention(emptySet())
    val outputName: Property<String> = objects.property(String::class.java).convention("cangjie_llvm_jni")
    val llvmDir: Property<String> = objects.property(String::class.java)
    val skipWhenLlvmMissing: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    fun sourceDir(path: Any) {
        sourceDirs.from(path)
    }

    fun includeDir(path: Any) {
        includeDirs.from(path)
    }
}

abstract class NativeCompileTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceDirs: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeDirs: ConfigurableFileCollection

    @get:Input
    abstract val compilerArgs: ListProperty<String>

    @get:Input
    abstract val linkerArgs: ListProperty<String>

    @get:Input
    abstract val definitions: SetProperty<String>

    @get:Input
    abstract val outputName: Property<String>

    @get:Input
    abstract val skipWhenLlvmMissing: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val llvmDir: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

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

class NativeCompilePlugin : Plugin<Project> {
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

private data class NativePlatform(
    val os: String,
    val arch: String,
    val objectSuffix: String,
    val macroName: String,
    val compileFlags: List<String>,
    val sharedLinkFlags: List<String>,
    val sharedLibraryExtension: String,
) {
    fun sharedLibraryFileName(baseName: String): String {
        return if (os == "windows") "$baseName.$sharedLibraryExtension" else "lib$baseName.$sharedLibraryExtension"
    }

    companion object {
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

private object LlvmLocator {
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

    private fun llvmConfigPrefix(): String? {
        return runCatching {
            val process = ProcessBuilder("llvm-config", "--prefix")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        }.getOrNull()
    }

    private fun isValidLlvmDir(dir: File): Boolean {
        if (!dir.exists()) return false
        val include = File(dir, "include/llvm-c/Core.h")
        val lib = File(dir, "lib")
        val lib64 = File(dir, "lib64")
        return include.exists() && (lib.exists() || lib64.exists())
    }
}

private object ToolchainResolver {
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

    fun resolveSharedLinkFlags(compiler: String, platform: NativePlatform): List<String> {
        if (platform.os != "windows") return platform.sharedLinkFlags
        val name = File(compiler).name.lowercase()
        return when {
            name == "cl.exe" || name == "clang-cl.exe" -> listOf("/DLL")
            else -> listOf("-shared")
        }
    }
}

private object JniHeaders {
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
