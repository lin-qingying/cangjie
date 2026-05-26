package org.cangnova.cangjie.analysis.test.framework.services.libraries

import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.getOrCreateTempDirectory
import org.cangnova.cangjie.test.services.sourceFileProvider
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

/**
 * 把测试模块编译成真实 `.cjo` binary roots。
 *
 * 当前仓颉 Analysis API 的真实 compiled library 场景依赖外部 `cjc -p ...`，
 * 因而这里直接对齐该 artifact 边界，而不是继续伪造 source-backed library module。
 */
abstract class TestModuleCompiler : TestService {
    abstract fun compileTestModuleToLibrary(
        module: TestModule,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
    ): CompiledLibrary
}

object CjcTestModuleCompiler : TestModuleCompiler() {
    override fun compileTestModuleToLibrary(
        module: TestModule,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
    ): CompiledLibrary {
        val moduleSlug = module.name.sanitizeFileName()
        val sourceRoot = testServices.getOrCreateTempDirectory("analysis-api-library-src-$moduleSlug").toPath()
        val outputRoot = testServices.getOrCreateTempDirectory("analysis-api-library-out-$moduleSlug").toPath()

        materializeModuleSources(module, sourceRoot, testServices)

        val command = buildList {
            add(findCjcPath().toString())
            add("-p")
            add(sourceRoot.toString())
            add("--output-type=staticlib")
            add("--no-sub-pkg")
            add("-o")
            add(outputRoot.toString())
            add("--diagnostic-format")
            add("json")
            dependencyBinaryRoots
                .distinct()
                .forEach { dependencyBinaryRoot ->
                    add("--import-path")
                    add(dependencyBinaryRoot.toString())
                }
        }

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            error(
                buildString {
                    appendLine("Failed to compile test module `${module.name}` to library with cjc.")
                    appendLine("Command: ${command.joinToString(" ")}")
                    appendLine("Exit code: $exitCode")
                    if (output.isNotBlank()) {
                        appendLine("Compiler output:")
                        append(output)
                    }
                },
            )
        }

        require(outputRoot.exists() && outputRoot.isDirectory()) {
            "cjc did not create the output directory for `${module.name}`: $outputRoot"
        }
        require(outputRoot.listDirectoryEntries().any { entry -> entry.fileName.toString().endsWith(".cjo", ignoreCase = true) }) {
            "cjc output for `${module.name}` does not contain any `.cjo` file under $outputRoot."
        }

        return CompiledLibrary(
            roots = listOf(outputRoot),
            sourceRoots = emptyList(),
        )
    }

    private fun materializeModuleSources(module: TestModule, sourceRoot: Path, testServices: TestServices) {
        module.files.forEach { testFile ->
            val relativePath = testFile.name.replace('\\', '/')
            val targetFile = sourceRoot.resolve(relativePath)
            Files.createDirectories(targetFile.parent)
            Files.writeString(
                targetFile,
                testServices.sourceFileProvider.getContentOfSourceFile(testFile),
                StandardCharsets.UTF_8,
            )
        }
    }

    private fun findCjcPath(): Path {
        val executableName = if (isWindows()) "cjc.exe" else "cjc"

        System.getenv("CANGJIE_HOME")
            ?.let(Path::of)
            ?.resolve("bin")
            ?.resolve(executableName)
            ?.takeIf(Files::exists)
            ?.let { return it }

        System.getProperty("cjc.home")
            ?.let(Path::of)
            ?.resolve("bin")
            ?.resolve(executableName)
            ?.takeIf(Files::exists)
            ?.let { return it }

        val userHome = Path.of(System.getProperty("user.home"))
        val candidateRoots = listOf(
            userHome.resolve(".cangjie").resolve("sdks"),
            userHome.resolve("sdk"),
        )

        candidateRoots.asSequence()
            .filter(Files::exists)
            .flatMap { root ->
                root.toFile()
                    .listFiles()
                    ?.sortedByDescending { it.name }
                    ?.asSequence()
                    ?.map { it.toPath() }
                    ?: emptySequence()
            }
            .map { sdkDir ->
                when {
                    Files.exists(sdkDir.resolve("bin").resolve(executableName)) -> sdkDir.resolve("bin").resolve(executableName)
                    Files.exists(sdkDir.resolve("cangjie").resolve("bin").resolve(executableName)) -> {
                        sdkDir.resolve("cangjie").resolve("bin").resolve(executableName)
                    }
                    else -> null
                }
            }
            .filterNotNull()
            .firstOrNull()
            ?.let { return it }

        error("Cannot find cjc. Set CANGJIE_HOME environment variable or -Dcjc.home=<sdk-home>.")
    }
}

private fun String.sanitizeFileName(): String =
    replace(Regex("""[^A-Za-z0-9_.-]"""), "_")

private fun isWindows(): Boolean =
    System.getProperty("os.name").contains("Windows", ignoreCase = true)
