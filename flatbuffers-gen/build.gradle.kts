import org.apache.tools.ant.taskdefs.condition.Os
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.flatbuffers.java)
}

abstract class LocateFlatcTask : DefaultTask() {
    @get:Input
    abstract val flatcVersion: Property<String>

    @get:Input
    abstract val flatcExeName: Property<String>

    @get:Input
    abstract val assetName: Property<String>

    @get:OutputFile
    abstract val flatcPath: RegularFileProperty

    @get:OutputFile
    abstract val zipFile: RegularFileProperty

    @TaskAction
    fun locate() {
        val exeName = flatcExeName.get()
        val version = flatcVersion.get()
        val targetFlatc = flatcPath.get().asFile
        val targetZip = zipFile.get().asFile
        val cacheDir = targetFlatc.parentFile

        val pathCandidates = System.getenv("PATH")
            ?.split(File.pathSeparator)
            ?.map { File(it, exeName) }
            .orEmpty()

        val flatcHome = System.getenv("FLATC_HOME")
        val homeCandidates = listOfNotNull(
            flatcHome?.let { File(it, "bin${File.separator}$exeName") },
            flatcHome?.let { File(it, exeName) }
        )

        val candidates = (homeCandidates + pathCandidates).filter { it.canExecute() }

        val systemFlatc = candidates.firstOrNull { exe ->
            try {
                val process = ProcessBuilder(exe.absolutePath, "--version")
                    .redirectErrorStream(true)
                    .start()
                val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
                process.waitFor()
                result.contains("flatc version") && result.contains(version)
            } catch (_: Exception) {
                false
            }
        }

        if (systemFlatc != null && systemFlatc.exists()) {
            logger.lifecycle("Found system flatc: ${systemFlatc.absolutePath}")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            if (!targetFlatc.exists()) {
                systemFlatc.copyTo(targetFlatc, overwrite = true)
                targetFlatc.setExecutable(true)
            }
            return
        }

        logger.lifecycle("No matching system flatc found, downloading from GitHub release")

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

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
    flatcPath.set(flatcPathProvider)
    zipFile.set(zipFileProvider)
}

val inputDir = layout.projectDirectory.dir("flatbuffers")
val outputDir = layout.projectDirectory.dir("gen")

outputDir.asFile.mkdirs()

tasks.register<Exec>("generateKotlinFlatBuffers") {
    dependsOn(locateFlatc)

    inputs.dir(inputDir)
    outputs.dir(outputDir)

    val schemaFiles = fileTree(inputDir.asFile) {
        include("**/*.fbs")
    }.files.map { it.absolutePath }

    commandLine(
        flatcPathProvider.get().asFile.absolutePath,
        "--kotlin",
        "--gen-mutable",
        "--gen-object-api",
        "-o",
        outputDir.asFile.absolutePath,
        *schemaFiles.toTypedArray(),
    )
}

tasks.compileKotlin {
    dependsOn("generateKotlinFlatBuffers")
}

sourceSets {
    "main" {
        generatedDir()
    }
}
