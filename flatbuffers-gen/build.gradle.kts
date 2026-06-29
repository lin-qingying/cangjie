import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.LocalState
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.flatbuffers.java)
}

/**
 * 定位并缓存当前平台可用的 flatc 可执行文件。
 *
 * 任务输出只暴露最终 flatc；下载 zip 属于本地状态，不参与下游任务输入，避免缺失 zip 时破坏 up-to-date 判断。
 */
@CacheableTask
abstract class LocateFlatcTask : DefaultTask() {
    @get:Input
    abstract val flatcVersion: Property<String>

    @get:Input
    abstract val flatcExeName: Property<String>

    @get:Input
    abstract val assetName: Property<String>

    @get:Input
    @get:Optional
    abstract val flatcHome: Property<String>

    @get:Input
    @get:Optional
    abstract val executableSearchPath: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configuredFlatcPath: RegularFileProperty

    @get:OutputFile
    abstract val flatcPath: RegularFileProperty

    @get:LocalState
    abstract val zipFile: RegularFileProperty

    @TaskAction
    fun locate() {
        val exeName = flatcExeName.get()
        val version = flatcVersion.get()
        val targetFlatc = flatcPath.get().asFile
        val targetZip = zipFile.get().asFile
        val cacheDir = targetFlatc.parentFile

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        configuredFlatcPath.asFile.orNull?.let { configuredFlatc ->
            if (!configuredFlatc.isFile) {
                throw GradleException("Configured flatc does not exist: ${configuredFlatc.absolutePath}")
            }
            if (!configuredFlatc.canExecute()) {
                throw GradleException("Configured flatc is not executable: ${configuredFlatc.absolutePath}")
            }
            if (!configuredFlatc.hasExpectedFlatcVersion(version)) {
                throw GradleException("Configured flatc version does not match $version: ${configuredFlatc.absolutePath}")
            }
            configuredFlatc.copyTo(targetFlatc, overwrite = true)
            targetFlatc.setExecutable(true, false)
            logger.lifecycle("Using configured flatc: ${configuredFlatc.absolutePath}")
            return
        }

        if (targetFlatc.isFile && targetFlatc.canExecute() && targetFlatc.hasExpectedFlatcVersion(version)) {
            logger.lifecycle("Reusing cached flatc: ${targetFlatc.absolutePath}")
            return
        }

        val pathCandidates = executableSearchPath.orNull
            ?.split(File.pathSeparator)
            ?.map { File(it, exeName) }
            .orEmpty()

        val flatcHome = flatcHome.orNull
        val homeCandidates = listOfNotNull(
            flatcHome?.let { File(it, "bin${File.separator}$exeName") },
            flatcHome?.let { File(it, exeName) }
        )

        val candidates = (homeCandidates + pathCandidates).filter { it.canExecute() }

        val systemFlatc = candidates.firstOrNull { it.hasExpectedFlatcVersion(version) }

        if (systemFlatc != null && systemFlatc.exists()) {
            logger.lifecycle("Found system flatc: ${systemFlatc.absolutePath}")
            systemFlatc.copyTo(targetFlatc, overwrite = true)
            targetFlatc.setExecutable(true, false)
            return
        }

        logger.lifecycle("No matching system flatc found, downloading from GitHub release")

        if (!targetZip.exists()) {
            val urlString = "https://github.com/google/flatbuffers/releases/download/v${version}/${assetName.get()}"
            val url = URI(urlString).toURL()
            logger.lifecycle("Downloading flatc from: $url")
            try {
                url.openStream().use { input ->
                    Files.newOutputStream(targetZip.toPath()).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                throw GradleException("Failed to download flatc", e)
            }
        }

        ZipInputStream(targetZip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            var extracted = false
            while (entry != null) {
                if (!entry.isDirectory && entry.name.substringAfterLast('/') == exeName) {
                    Files.newOutputStream(targetFlatc.toPath()).use { out ->
                        zis.copyTo(out)
                    }
                    extracted = true
                    break
                }
                entry = zis.nextEntry
            }
            if (!extracted) {
                throw GradleException("flatc executable not found in archive: ${targetZip.absolutePath}")
            }
        }

        targetFlatc.setExecutable(true, false)
        targetFlatc.setReadable(true, false)
        targetFlatc.setWritable(true, false)

        if (!targetFlatc.exists() || !targetFlatc.canExecute()) {
            throw GradleException("flatc is not executable: ${targetFlatc.absolutePath}")
        }

        logger.lifecycle("flatc is ready at: ${targetFlatc.absolutePath}")
    }

    private fun File.hasExpectedFlatcVersion(version: String): Boolean {
        return try {
            val process = ProcessBuilder(absolutePath, "--version")
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            result.contains("flatc version") && result.contains(version)
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * 使用已定位的 flatc 根据 schema 生成 Kotlin FlatBuffers 源码。
 *
 * schema、flatc 二进制和生成选项全部作为任务输入，输出目录作为唯一产物参与 Gradle 缓存。
 */
@CacheableTask
abstract class GenerateKotlinFlatBuffersTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val flatcPath: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schemaFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val generateMutableObjects: Property<Boolean>

    @get:Input
    abstract val generateObjectApi: Property<Boolean>

    init {
        generateMutableObjects.convention(true)
        generateObjectApi.convention(true)
    }

    @TaskAction
    fun generate() {
        val output = outputDir.get().asFile
        fileSystemOperations.delete {
            delete(output)
        }
        output.mkdirs()

        val args = buildList {
            add(flatcPath.get().asFile.absolutePath)
            add("--kotlin")
            if (generateMutableObjects.get()) {
                add("--gen-mutable")
            }
            if (generateObjectApi.get()) {
                add("--gen-object-api")
            }
            add("-o")
            add(output.absolutePath)
            addAll(schemaFiles.files.sortedBy { it.absolutePath }.map { it.absolutePath })
        }

        execOperations.exec {
            commandLine(args)
        }
    }
}

val resolvedFlatcVersion = "25.2.10"
val resolvedFlatcExeName = "flatc" + if (Os.isFamily(Os.FAMILY_WINDOWS)) ".exe" else ""
val resolvedAssetName = run {
    val os = when {
        Os.isFamily(Os.FAMILY_WINDOWS) -> "Windows"
        Os.isFamily(Os.FAMILY_MAC) && Os.isArch("arm64") -> "MacIntel"
        Os.isFamily(Os.FAMILY_MAC) -> "Mac"
        else -> "Linux"
    }

    val compilerSuffix = if (Os.isFamily(Os.FAMILY_UNIX)) ".clang++-18" else ""
    "$os.flatc.binary$compilerSuffix.zip"
}

val cacheDirProvider = layout.buildDirectory.dir("flatc")
val flatcPathProvider = cacheDirProvider.map { it.file(resolvedFlatcExeName) }
val zipFileProvider = cacheDirProvider.map { it.file(resolvedAssetName) }

val locateFlatc = tasks.register<LocateFlatcTask>("locateFlatc") {
    this.flatcVersion.set(resolvedFlatcVersion)
    this.flatcExeName.set(resolvedFlatcExeName)
    this.assetName.set(resolvedAssetName)
    flatcHome.set(providers.environmentVariable("FLATC_HOME"))
    executableSearchPath.set(providers.environmentVariable("PATH"))
    providers.gradleProperty("cangjie.flatc.path").orNull?.let {
        configuredFlatcPath.fileValue(file(it))
    }
    flatcPath.set(flatcPathProvider)
    zipFile.set(zipFileProvider)
}

val inputDir = layout.projectDirectory.dir("flatbuffers")
val flatbuffersOutputDir = layout.projectDirectory.dir("gen")

tasks.register<GenerateKotlinFlatBuffersTask>("generateKotlinFlatBuffers") {
    dependsOn(locateFlatc)
    flatcPath.set(flatcPathProvider)
    schemaFiles.from(fileTree(inputDir.asFile) {
        include("**/*.fbs")
    })
    outputDir.set(flatbuffersOutputDir)
}

tasks.compileKotlin {
    dependsOn("generateKotlinFlatBuffers")
}

sourceSets {
    "main" {
        generatedDir()
    }
}
