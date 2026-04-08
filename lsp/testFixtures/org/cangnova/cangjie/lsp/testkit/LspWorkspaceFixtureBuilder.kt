package org.cangnova.cangjie.lsp.testkit

import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * 真实语义测试使用的工作区夹具构造器。
 *
 * 目标是把“临时工作区 + stdlib fixture + multiModule 初始化参数”收敛到一个地方，
 * 避免每个测试类都自己拼目录、复制 stdlib、组装 initializationOptions。
 */
class LspWorkspaceFixtureBuilder {
    private val files = linkedMapOf<String, String>()
    private val modules = linkedMapOf<String, ModuleSpec>()
    private var includeStdlib: Boolean = true

    init {
        addModule(name = "workspace")
    }

    fun addModule(
        name: String,
        rootRelativePath: String = "",
        sourceRoots: List<String> = listOf("src"),
        outputRelativePath: String = "target",
        packageSearchPaths: List<String> = emptyList(),
    ): LspWorkspaceFixtureBuilder {
        modules[normalizeRelative(rootRelativePath)] = ModuleSpec(
            name = name,
            rootRelativePath = normalizeRelative(rootRelativePath),
            sourceRoots = sourceRoots.map(::normalizeRelative),
            outputRelativePath = normalizeRelative(outputRelativePath),
            packageSearchPaths = packageSearchPaths,
        )
        return this
    }

    fun file(relativePath: String, text: String): LspWorkspaceFixtureBuilder {
        files[normalizeRelative(relativePath)] = text
        return this
    }

    fun source(relativePath: String, text: String): LspWorkspaceFixtureBuilder {
        return file("src/$relativePath", text)
    }

    fun withoutStdlib(): LspWorkspaceFixtureBuilder {
        includeStdlib = false
        return this
    }

    fun build(): LspWorkspaceFixture {
        val workspaceRoot = Files.createTempDirectory("cangjie-lsp-workspace-")
        val cacheRoot = workspaceRoot.resolve(".cache").resolve("lsp").createDirectories()
        val stdlibRoot = if (includeStdlib) {
            val extracted = Files.createTempDirectory("cangjie-lsp-stdlib-")
            copyStdlibFixtures(extracted)
            extracted
        } else {
            null
        }

        files.forEach { (relativePath, text) ->
            val filePath = workspaceRoot.resolve(relativePath)
            filePath.parent?.createDirectories()
            filePath.writeText(text)
        }

        modules.values.forEach { module ->
            val outputDirectory = workspaceRoot
                .resolve(module.rootRelativePath)
                .resolve(module.outputRelativePath)
            outputDirectory.createDirectories()
        }

        return LspWorkspaceFixture(
            workspaceRoot = workspaceRoot,
            cacheRoot = cacheRoot,
            stdlibRoot = stdlibRoot,
            modules = modules.values.toList(),
        )
    }

    private fun copyStdlibFixtures(destinationRoot: Path) {
        val stdFixtureRoot = locateRepositoryRoot()
            .resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")

        copyFile(stdFixtureRoot.resolve("std.cjo"), destinationRoot.resolve("std.cjo"))
        copyFile(
            stdFixtureRoot.resolve("std").resolve("std.core.cjo"),
            destinationRoot.resolve("std").resolve("std.core.cjo"),
        )
    }

    private fun copyFile(source: Path, target: Path) {
        target.parent?.createDirectories()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun locateRepositoryRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (current != current.root) {
            if (current.resolve("settings.gradle.kts").exists()) {
                return current
            }
            current = current.parent
        }
        error("Cannot locate repository root from ${Path.of("").toAbsolutePath().invariantSeparatorsPathString}")
    }

    private fun normalizeRelative(relativePath: String): String {
        return relativePath
            .replace('\\', '/')
            .removePrefix("./")
            .trim('/')
    }
}

class LspWorkspaceFixture internal constructor(
    val workspaceRoot: Path,
    val cacheRoot: Path,
    val stdlibRoot: Path?,
    private val modules: List<ModuleSpec>,
) : AutoCloseable {
    fun path(relativePath: String): Path {
        return workspaceRoot.resolve(
            relativePath.replace('\\', '/').removePrefix("./"),
        )
    }

    fun uri(relativePath: String): String = path(relativePath).toUri().toString()

    fun text(relativePath: String): String = path(relativePath).readText()

    fun workspaceFolder(relativePath: String = ""): WorkspaceFolder {
        val folderPath = if (relativePath.isBlank()) workspaceRoot else path(relativePath)
        return WorkspaceFolder(folderPath.toUri().toString(), folderPath.fileName?.toString() ?: "workspace")
    }

    fun initializeParams(
        capabilities: ClientCapabilities = LspClientCapabilitiesBuilder.fullFeatured(),
        workspaceFolders: List<WorkspaceFolder> = listOf(workspaceFolder()),
        includeMultiModuleOption: Boolean = true,
    ): InitializeParams {
        val rootUri = workspaceFolders.firstOrNull()?.uri ?: workspaceRoot.toUri().toString()
        return InitializeParams().apply {
            this.rootUri = rootUri
            this.capabilities = capabilities
            this.workspaceFolders = workspaceFolders
            initializationOptions = buildInitializationOptions(
                workspaceFolders = workspaceFolders,
                includeMultiModuleOption = includeMultiModuleOption,
            )
        }
    }

    override fun close() {
        workspaceRoot.toFile().deleteRecursively()
        stdlibRoot?.toFile()?.deleteRecursively()
    }

    private fun buildInitializationOptions(
        workspaceFolders: List<WorkspaceFolder>,
        includeMultiModuleOption: Boolean,
    ): Map<String, Any> {
        val options = linkedMapOf<String, Any>(
            "targetLib" to cacheRoot.toString(),
        )
        stdlibRoot?.let { options["stdLibPathOption"] = it.toString() }
        if (includeMultiModuleOption) {
            options["multiModuleOption"] = modules.associate { module ->
                val moduleRoot = workspaceRoot.resolve(module.rootRelativePath.ifBlank { "." }).normalize()
                moduleRoot.toUri().toString() to mapOf(
                    "name" to module.name,
                    "package_requires" to mapOf(
                        "path_option" to module.packageSearchPaths,
                        "package_option" to emptyMap<String, String>(),
                    ),
                    "source_sets" to mapOf(
                        "main" to mapOf(
                            "source_roots" to module.sourceRoots.map { sourceRoot ->
                                moduleRoot.resolve(sourceRoot.ifBlank { "." }).normalize().toUri().toString()
                            },
                            "resource_roots" to emptyList<String>(),
                            "output_directory" to listOf(
                                moduleRoot.resolve(module.outputRelativePath).normalize().toUri().toString(),
                            ),
                            "is_test" to false,
                        ),
                    ),
                )
            }
        } else if (workspaceFolders.isEmpty()) {
            options.remove("targetLib")
        }
        return options
    }
}

internal data class ModuleSpec(
    val name: String,
    val rootRelativePath: String,
    val sourceRoots: List<String>,
    val outputRelativePath: String,
    val packageSearchPaths: List<String>,
)
