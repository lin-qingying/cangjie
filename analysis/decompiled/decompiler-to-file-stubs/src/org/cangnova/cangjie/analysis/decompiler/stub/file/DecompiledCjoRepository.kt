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
 * [moduleKey] 区分不同 library/builtins module，[roots] 描述该模块可参与搜索的物理根目录，
 * [builtinsRoots] 描述 SDK builtins 根目录；三者共同决定 [CjoManager] 的搜索路径。
 */
internal data class RepositoryKey(
    /** 项目结构中 module 的稳定标识，用于隔离同名包在不同模块中的二进制来源。 */
    val moduleKey: String,

    /** 当前 module 可搜索的 `.cjo` 根目录集合，已经在调用方完成规范化。 */
    val roots: List<File>,

    /** SDK builtins 根目录集合，反序列化期间解析跨库符号时与模块根一起参与搜索。 */
    val builtinsRoots: List<File> = emptyList(),
)

/**
 * 面向单个 module roots 集合的 `.cjo` 反编译仓库。
 *
 * 该类封装 [CjoManager] 的搜索路径装配，并把 package 与 header 一起加载为
 * [LoadedCjoPackage]，供后续 file-stub 构建和反编译文本渲染共享。
 */
internal class DecompiledCjoRepository(
    roots: List<File>,
    builtinsRoots: List<File> = emptyList(),
) {
    /**
     * 反序列化期间解析符号时必须覆盖的包全集：模块根 + builtins 根。
     *
     * 官方仓颉编译器 `CjoManagerImpl::UpdateSearchPath`（`external/cangjie_compiler/src/Modules/CjoManagerImpl.h:80-88`）把库搜索路径拼为
     * 用户 import 路径 + `environment.cangjiePaths`（stdlib）+ 当前模块路径，包号在这个并集里查找，而不是只在当前模块根下查找；
     * IDE 的 builtins 仓库只按单个 `.cjo` 的父目录建根（`DecompiledPackageDataFinder`），只看模块根会漏掉 `std` 根包里的声明（如 `std.Frozen`）。
     * 这里把 builtins 根并入搜索路径：`std` 包先查 builtins 根，其余包查模块根 + builtins 根，与 [CjoSearchPath.searchPathsFor] 语义一致，全程不读 PSI。
     * 两个搜索根的名字（`CANGJIE_STDLIB_MODULE` / `CANGJIE_LIBRARY`）是本仓 `CjoSearchPath` 与 SDK 工具链的环境变量命名，官方 C++ 源码里对应的是上述 `importPaths` 与 `cangjiePaths` 两段路径。
     */
    private val libraryRootPathString = (roots + builtinsRoots)
        .distinctBy(File::getAbsolutePath)
        .joinToString(File.pathSeparator) { it.absolutePath }

    /** builtins 搜索根；`std` 系列包先查这里。 */
    private val builtinsRootPathString = builtinsRoots
        .distinctBy(File::getAbsolutePath)
        .joinToString(File.pathSeparator) { it.absolutePath }

    /** 负责按包名读取 `.cjo` package 与 package header 的序列化管理器。 */
    private val cjoManager = CjoManager(
        CjoSearchPath { key ->
            when (key) {
                "CANGJIE_STDLIB_MODULE" -> builtinsRootPathString
                "CANGJIE_LIBRARY" -> libraryRootPathString
                else -> null
            }
        },
    )

    /**
     * 从当前仓库根目录中加载指定包的 `.cjo` 数据。
     *
     * 只有 package body 与 package header 都能成功读取时才返回结果；返回值携带原始虚拟文件、
     * 包名、当前 repository manager 和版本兼容性，后续层复用同一包快照及其跨包索引。
     */
    fun loadPackageData(
        packageFqName: FqName,
        binaryFile: VirtualFile,
    ): LoadedCjoPackage? {
        val fullPkgName = packageFqName.asString()
        val loaded = cjoManager.loadPackageSnapshot(fullPkgName) ?: return null
        val pkg = loaded.pkg
        val header = loaded.header
        return LoadedCjoPackage(
            binaryFile = binaryFile,
            packageFqName = packageFqName,
            pkg = pkg,
            header = header,
            cjoManager = cjoManager,
            isVersionSupported = CjoBinaryFileReader.isSupportedVersion(pkg),
            sourcePath = loaded.sourcePath,
        )
    }
}
