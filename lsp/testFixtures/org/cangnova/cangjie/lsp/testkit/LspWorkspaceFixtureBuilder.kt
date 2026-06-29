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
    /**
     * 待写入临时工作区的文件内容。
     */
    private val files = linkedMapOf<String, String>()

    /**
     * 待写入初始化参数的模块定义。
     */
    private val modules = linkedMapOf<String, ModuleSpec>()

    /**
     * 是否复制标准库 cjo 夹具。
     */
    private var includeStdlib: Boolean = true

    init {
        addModule(name = "workspace")
    }

    /**
     * 添加或替换一个工作区模块定义。
     */
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

    /**
     * 添加一个工作区文件。
     */
    fun file(relativePath: String, text: String): LspWorkspaceFixtureBuilder {
        files[normalizeRelative(relativePath)] = text
        return this
    }

    /**
     * 在默认 `src` 源码根下添加一个仓颉源码文件。
     */
    fun source(relativePath: String, text: String): LspWorkspaceFixtureBuilder {
        return file("src/$relativePath", text)
    }

    /**
     * 禁用标准库 cjo 夹具复制。
     */
    fun withoutStdlib(): LspWorkspaceFixtureBuilder {
        includeStdlib = false
        return this
    }

    /**
     * 创建临时工作区并写入所有声明的文件、模块输出目录和标准库夹具。
     */
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

    /**
     * 复制最小标准库 cjo 夹具到目标目录。
     */
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

    /**
     * 复制单个文件并自动创建父目录。
     */
    private fun copyFile(source: Path, target: Path) {
        target.parent?.createDirectories()
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    }

    /**
     * 从当前目录向上查找仓库根目录。
     */
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

    /**
     * 规范化夹具内部使用的相对路径。
     */
    private fun normalizeRelative(relativePath: String): String {
        return relativePath
            .replace('\\', '/')
            .removePrefix("./")
            .trim('/')
    }
}

/**
 * 已创建的 LSP 临时工作区夹具。
 */
class LspWorkspaceFixture internal constructor(
    /**
     * 临时工作区根目录。
     */
    val workspaceRoot: Path,

    /**
     * LSP 测试使用的目标库缓存目录。
     */
    val cacheRoot: Path,

    /**
     * 标准库 cjo 夹具目录；禁用标准库时为 null。
     */
    val stdlibRoot: Path?,

    /**
     * 工作区模块定义列表。
     */
    private val modules: List<ModuleSpec>,
) : AutoCloseable {
    /**
     * 将相对路径解析为临时工作区下的真实路径。
     */
    fun path(relativePath: String): Path {
        return workspaceRoot.resolve(
            relativePath.replace('\\', '/').removePrefix("./"),
        )
    }

    /**
     * 返回相对路径对应的 file URI。
     */
    fun uri(relativePath: String): String = path(relativePath).toUri().toString()

    /**
     * 读取相对路径对应文件的文本。
     */
    fun text(relativePath: String): String = path(relativePath).readText()

    /**
     * 构造指定相对路径对应的 workspace folder。
     */
    fun workspaceFolder(relativePath: String = ""): WorkspaceFolder {
        val folderPath = if (relativePath.isBlank()) workspaceRoot else path(relativePath)
        return WorkspaceFolder(folderPath.toUri().toString(), folderPath.fileName?.toString() ?: "workspace")
    }

    /**
     * 构造该工作区对应的 initialize 参数。
     */
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

    /**
     * 删除临时工作区和可选标准库夹具目录。
     */
    override fun close() {
        workspaceRoot.toFile().deleteRecursively()
        stdlibRoot?.toFile()?.deleteRecursively()
    }

    /**
     * 构造 initializationOptions 中的仓颉工程配置。
     */
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

/**
 * 测试工作区中的模块规格。
 */
internal data class ModuleSpec(
    /**
     * 模块名称。
     */
    val name: String,

    /**
     * 模块根目录相对工作区根的路径。
     */
    val rootRelativePath: String,

    /**
     * 模块源码根相对模块根的路径列表。
     */
    val sourceRoots: List<String>,

    /**
     * 模块输出目录相对模块根的路径。
     */
    val outputRelativePath: String,

    /**
     * 模块 package 依赖搜索路径。
     */
    val packageSearchPaths: List<String>,
)
