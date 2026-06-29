package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.name.FqName
import java.io.File

/**
 * 反编译 `.cjo` 仓库缓存的键。
 *
 * [moduleKey] 区分不同 library/builtins module，[roots] 描述该模块可参与搜索的物理根目录；
 * 二者共同决定 [CjoManager] 的搜索路径，避免不同模块共享错误的 package binary。
 */
internal data class RepositoryKey(
    /** 项目结构中 module 的稳定标识，用于隔离同名包在不同模块中的二进制来源。 */
    val moduleKey: String,

    /** 当前 module 可搜索的 `.cjo` 根目录集合，已经在调用方完成规范化。 */
    val roots: List<File>,
)

/**
 * 面向单个 module roots 集合的 `.cjo` 反编译仓库。
 *
 * 该类封装 [CjoManager] 的搜索路径装配，并把 package 与 header 一起加载为
 * [LoadedCjoPackage]，供后续 file-stub 构建和反编译文本渲染共享。
 */
internal class DecompiledCjoRepository(
    roots: List<File>,
) {
    /** 传给 [CjoSearchPath] 的平台分隔搜索路径字符串。 */
    private val rootPathString = roots.joinToString(File.pathSeparator) { it.absolutePath }

    /** 负责按包名读取 `.cjo` package 与 package header 的序列化管理器。 */
    private val cjoManager = CjoManager(
        CjoSearchPath { key ->
            when (key) {
                "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> rootPathString
                else -> null
            }
        },
    )

    /**
     * 从当前仓库根目录中加载指定包的 `.cjo` 数据。
     *
     * 只有 package body 与 package header 都能成功读取时才返回结果；返回值同时携带原始虚拟文件、
     * 包名、搜索根和版本兼容性，后续层不需要重新访问底层 [CjoManager]。
     */
    fun loadPackageData(
        packageFqName: FqName,
        binaryFile: VirtualFile,
        searchRoots: List<File>,
    ): LoadedCjoPackage? {
        val fullPkgName = packageFqName.asString()
        val pkg = cjoManager.loadPackage(fullPkgName) ?: return null
        val header = cjoManager.loadPackageHeader(fullPkgName) ?: return null
        return LoadedCjoPackage(
            binaryFile = binaryFile,
            packageFqName = packageFqName,
            pkg = pkg,
            header = header,
            searchRoots = searchRoots,
            isVersionSupported = CjoBinaryFileReader.isSupportedVersion(pkg),
        )
    }
}
