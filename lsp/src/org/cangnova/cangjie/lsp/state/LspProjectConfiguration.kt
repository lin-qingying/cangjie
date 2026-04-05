package org.cangnova.cangjie.lsp.state

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
    val workspaceModules: List<LspWorkspaceModuleDefinition>,
    val stdlibSearchPaths: List<String>,
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
        const val STDLIB_PROPERTY: String = "cangjie.stdlib.module"
        const val LIBRARY_PROPERTY: String = "cangjie.library"

        fun fromInitializeParams(
            params: InitializeParams,
            workspaceFoldersOverride: List<WorkspaceFolder>? = null,
        ): LspProjectConfiguration {
            val initializationOptions = params.initializationOptions as? Map<*, *> ?: emptyMap<Any, Any>()
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

        private fun parsePackageSearchPaths(moduleMap: Map<*, *>): List<String> {
            val packageRequires = moduleMap["package_requires"] as? Map<*, *> ?: return emptyList()
            return (packageRequires["path_option"] as? List<*>)
                .orEmpty()
                .mapNotNull { it.asNonBlankStringOrNull() }
                .distinct()
        }

        private fun inferWorkspaceName(uri: String): String {
            val trimmed = uri.trimEnd('/')
            val slashIndex = trimmed.lastIndexOf('/')
            return if (slashIndex >= 0) trimmed.substring(slashIndex + 1) else trimmed
        }

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

data class LspWorkspaceModuleDefinition(
    val name: String,
    val sourceRootUris: List<String>,
    val packageSearchPaths: List<String>,
)

internal fun Any?.asNonBlankStringOrNull(): String? =
    (this as? String)?.takeIf { it.isNotBlank() }

internal fun String.toPathOrNull(): Path? =
    runCatching { Paths.get(this) }.getOrNull()

internal fun String.uriToPathOrNull(): Path? =
    runCatching { Paths.get(URI(this)) }.getOrNull()
