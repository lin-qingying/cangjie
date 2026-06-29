package org.cangnova.cangjie.lsp.state

import com.google.gson.Gson
import com.google.gson.JsonElement
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

/**
 * LSP 初始化阶段抽取出的工程级配置。
 *
 * 这层配置不是为了“把 JSON 塞进业务代码”，而是把客户端协商出来的项目模型
 * 统一沉淀成服务端内部的稳定描述，供：
 * 1. project structure 构建工作区模块图；
 * 2. 标准库 / 三方库搜索路径注入；
 * 3. 后续增量文档与工程模型对齐。
 */
data class LspProjectConfiguration(
    /**
     * 当前工作区中客户端声明或推导出的模块定义。
     */
    val workspaceModules: List<LspWorkspaceModuleDefinition>,

    /**
     * 标准库 `.cjo` 或模块搜索路径。
     */
    val stdlibSearchPaths: List<String>,

    /**
     * 三方库或 package 依赖搜索路径。
     */
    val librarySearchPaths: List<String>,
) {
    /**
     * CFIR `.cjo` 搜索路径在当前仓库中通过环境变量语义建模。
     *
     * LSP 客户端不会替服务端设置进程级环境，因此这里把协商结果收敛到系统属性，
     * 由底层 session cache 统一按“属性优先、环境变量兜底”的方式读取。
     */
    fun applyLibrarySearchProperties() {
        setOrClearSystemProperty(STDLIB_PROPERTY, stdlibSearchPaths)
        setOrClearSystemProperty(LIBRARY_PROPERTY, librarySearchPaths)
    }

    companion object {
        /**
         * 标准库搜索路径写入的系统属性名。
         */
        const val STDLIB_PROPERTY: String = "cangjie.stdlib.module"

        /**
         * 三方库搜索路径写入的系统属性名。
         */
        const val LIBRARY_PROPERTY: String = "cangjie.library"

        /**
         * 从 LSP initialize 参数构造工程配置。
         *
         * 方法解析 initializationOptions、workspaceFolders/rootUri、stdlib 和 library 路径，
         * 并在工作区目录覆盖参数存在时使用最新目录状态重建模块列表。
         */
        fun fromInitializeParams(
            params: InitializeParams,
            workspaceFoldersOverride: List<WorkspaceFolder>? = null,
        ): LspProjectConfiguration {
            val initializationOptions = normalizeInitializationOptions(params.initializationOptions)
            val workspaceFolders = workspaceFoldersOverride
                ?: params.workspaceFolders
                ?: params.rootUri?.let { listOf(WorkspaceFolder(it, inferWorkspaceName(it))) }
                ?: emptyList()

            val workspaceModules = parseWorkspaceModules(
                rawInitializationOptions = initializationOptions,
                workspaceFolders = workspaceFolders,
            )

            val stdlibSearchPaths = buildList {
                initializationOptions["stdLibPathOption"]
                    .asNonBlankStringOrNull()
                    ?.let(::add)
            }.distinct()

            val librarySearchPaths = buildList {
                initializationOptions["targetLib"]
                    .asNonBlankStringOrNull()
                    ?.let(::add)

                workspaceModules.forEach { module ->
                    addAll(module.packageSearchPaths)
                }
            }.distinct()

            return LspProjectConfiguration(
                workspaceModules = workspaceModules,
                stdlibSearchPaths = stdlibSearchPaths,
                librarySearchPaths = librarySearchPaths,
            )
        }

        /**
         * 从初始化选项和 workspace folder 中解析工作区模块定义。
         *
         * 客户端未提供 multi-module 配置时，每个 workspace folder 会退化为一个模块。
         */
        private fun parseWorkspaceModules(
            rawInitializationOptions: Map<*, *>,
            workspaceFolders: List<WorkspaceFolder>,
        ): List<LspWorkspaceModuleDefinition> {
            val rawMultiModule = rawInitializationOptions["multiModuleOption"] as? Map<*, *>
            if (rawMultiModule.isNullOrEmpty()) {
                return workspaceFolders.map { folder ->
                    LspWorkspaceModuleDefinition(
                        name = folder.name.ifBlank { inferWorkspaceName(folder.uri) },
                        sourceRootUris = listOf(folder.uri),
                        packageSearchPaths = emptyList(),
                    )
                }
            }

            return rawMultiModule.entries.mapNotNull { (moduleUri, moduleValue) ->
                val moduleRootUri = moduleUri as? String ?: return@mapNotNull null
                val moduleMap = moduleValue as? Map<*, *> ?: return@mapNotNull null
                val sourceRootUris = parseSourceRootUris(moduleMap, moduleRootUri)
                val packageSearchPaths = parsePackageSearchPaths(moduleMap)
                LspWorkspaceModuleDefinition(
                    name = moduleMap["name"].asNonBlankStringOrNull() ?: inferWorkspaceName(moduleRootUri),
                    sourceRootUris = sourceRootUris.ifEmpty { listOf(moduleRootUri) },
                    packageSearchPaths = packageSearchPaths,
                )
            }
        }

        /**
         * 解析单个模块配置中的源码根 URI。
         *
         * 未声明 source_sets 时以模块根 URI 作为默认源码根。
         */
        private fun parseSourceRootUris(
            moduleMap: Map<*, *>,
            moduleRootUri: String,
        ): List<String> {
            val sourceSets = moduleMap["source_sets"] as? Map<*, *> ?: return listOf(moduleRootUri)
            return sourceSets.values
                .asSequence()
                .mapNotNull { it as? Map<*, *> }
                .flatMap { sourceSet ->
                    (sourceSet["source_roots"] as? List<*>)
                        .orEmpty()
                        .asSequence()
                        .mapNotNull { it.asNonBlankStringOrNull() }
                }
                .distinct()
                .toList()
        }

        /**
         * 解析模块 package 依赖的搜索路径。
         */
        private fun parsePackageSearchPaths(moduleMap: Map<*, *>): List<String> {
            val packageRequires = moduleMap["package_requires"] as? Map<*, *> ?: return emptyList()
            return (packageRequires["path_option"] as? List<*>)
                .orEmpty()
                .mapNotNull { it.asNonBlankStringOrNull() }
                .distinct()
        }

        /**
         * 从 URI 尾段推导工作区或模块名称。
         */
        private fun inferWorkspaceName(uri: String): String {
            val trimmed = uri.trimEnd('/')
            val slashIndex = trimmed.lastIndexOf('/')
            return if (slashIndex >= 0) trimmed.substring(slashIndex + 1) else trimmed
        }

        /**
         * 将 initializationOptions 规范化为 Map。
         *
         * 输入可能来自 lsp4j 的 Map、Gson JsonElement、JSON 字符串或其他可序列化对象。
         */
        private fun normalizeInitializationOptions(raw: Any?): Map<*, *> {
            val gson = Gson()
            return when (raw) {
                is Map<*, *> -> raw
                is JsonElement -> runCatching { gson.fromJson(raw, Map::class.java) as? Map<*, *> }.getOrNull().orEmpty()
                is String -> runCatching { gson.fromJson(raw, Map::class.java) as? Map<*, *> }.getOrNull().orEmpty()
                else -> runCatching { gson.fromJson(gson.toJson(raw), Map::class.java) as? Map<*, *> }.getOrNull().orEmpty()
            }
        }

        /**
         * 根据路径列表设置或清除系统属性。
         *
         * 多个路径按当前平台的 path separator 拼接，空列表则清除属性以避免污染后续会话。
         */
        private fun setOrClearSystemProperty(
            key: String,
            values: List<String>,
        ) {
            if (values.isEmpty()) {
                System.clearProperty(key)
            } else {
                System.setProperty(key, values.joinToString(File.pathSeparator))
            }
        }
    }
}

/**
 * LSP 工作区中的单个模块定义。
 *
 * 模块定义描述源码根和 package 搜索路径，是 Analysis API 项目结构构建模块图的输入。
 */
data class LspWorkspaceModuleDefinition(
    /**
     * 模块名称。
     */
    val name: String,

    /**
     * 该模块包含的源码根 URI。
     */
    val sourceRootUris: List<String>,

    /**
     * 该模块额外贡献的 package 依赖搜索路径。
     */
    val packageSearchPaths: List<String>,
)

/**
 * 将对象转换为非空字符串。
 *
 * 非字符串或空白字符串返回 null，用于解析宽松 initializationOptions。
 */
internal fun Any?.asNonBlankStringOrNull(): String? =
    (this as? String)?.takeIf { it.isNotBlank() }

/**
 * 将普通路径字符串转换为 [Path]，失败时返回 null。
 */
internal fun String.toPathOrNull(): Path? =
    runCatching { Paths.get(this) }.getOrNull()

/**
 * 将 URI 字符串转换为 [Path]，失败时返回 null。
 */
internal fun String.uriToPathOrNull(): Path? =
    runCatching { Paths.get(URI(this)) }.getOrNull()
