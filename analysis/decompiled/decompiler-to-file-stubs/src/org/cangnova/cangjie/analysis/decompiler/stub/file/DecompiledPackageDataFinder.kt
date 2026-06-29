@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.decompiler.stub.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.platform.presentableDescription
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` package 数据查找器。
 *
 * 只负责按 binary / module / package 恢复反编译所需的 package 数据，
 * 不再承担 binary 索引职责。
 */
class DecompiledPackageDataFinder(
    /**
     * 提供反编译 binary 索引、模块结构和修改计数服务的 IntelliJ 项目实例。
     */
    private val project: Project,
) {
    /**
     * 上次同步仓库缓存时观察到的项目结构修改计数。
     *
     * 当该值与 [CaModificationTracker.modificationCount] 不一致时，所有按 module roots 缓存的
     * [DecompiledCjoRepository] 都需要失效，避免继续使用旧 binary roots。
     */
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    /**
     * 按 module key 与搜索根缓存的 `.cjo` 仓库实例。
     */
    private val repositories = ConcurrentHashMap<RepositoryKey, DecompiledCjoRepository>()

    /**
     * 当前项目的反编译二进制索引。
     *
     * 索引负责从虚拟文件反查包名与所属模块，finder 只在拿到这些结构化信息后加载 package 数据。
     */
    private val binaryIndex: CaDecompiledBinaryIndex
        get() = project.getService(CaDecompiledBinaryIndex::class.java)

    /**
     * 根据实际 `.cjo` 虚拟文件加载其 package 数据。
     *
     * 该入口用于 decompiler 从文件出发恢复声明：先通过 [binaryIndex] 找到包名和 owner module，
     * 再分派到 library 或 builtins 的 module-aware 加载路径。
     */
    fun loadPackageData(binaryFile: VirtualFile): LoadedCjoPackage? {
        refreshIfNeeded()
        val packageFqName = binaryIndex.readPackageFqName(binaryFile) ?: return null
        val module = binaryIndex.findOwningModule(binaryFile) ?: return null
        return when (module) {
            is CaLibraryModule -> loadPackageData(module, packageFqName)
            is CaBuiltinsModule -> loadPackageData(
                moduleKey = "<builtins:${module.targetPlatform.presentableDescription}>",
                packageFqName = packageFqName,
                binaryFile = binaryFile,
                rootFiles = listOf(binaryFile),
            )
            else -> null
        }
    }

    /**
     * 根据 library module 与包名加载 `.cjo` package 数据。
     *
     * Library module 使用其 binary roots 作为搜索范围，并以稳定 module 名参与缓存键计算，
     * 以支持不同依赖中同名包的独立反编译。
     */
    fun loadPackageData(module: CaLibraryModule, packageFqName: FqName): LoadedCjoPackage? {
        refreshIfNeeded()
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        val roots = module.binaryRoots
            .mapNotNull { it.virtualFile }
            .map(::toRootFile)
            .map(::normalizeRoot)
            .distinctBy(File::getAbsolutePath)
        return repositoryFor(module.stableModuleName ?: module.moduleDescription, roots)
            .loadPackageData(packageFqName, binaryFile, roots)
    }

    /**
     * 根据 builtins module 与包名加载 `.cjo` package 数据。
     *
     * Builtins 以目标平台描述构造独立 module key，并以当前 `.cjo` 文件所在目录作为搜索根，
     * 确保标准库内建包不会与普通 library roots 混用。
     */
    fun loadPackageData(module: CaBuiltinsModule, packageFqName: FqName): LoadedCjoPackage? {
        refreshIfNeeded()
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return loadPackageData(
            moduleKey = "<builtins:${module.targetPlatform.presentableDescription}>",
            packageFqName = packageFqName,
            binaryFile = binaryFile,
            rootFiles = listOf(binaryFile),
        )
    }

    /**
     * 使用显式 module key 与 root files 加载 `.cjo` package 数据。
     *
     * 该共享路径服务于 builtins 以及从 binary file 反推出的 module 场景，负责把虚拟根规范化为
     * 物理根目录并复用 [repositoryFor] 中的仓库缓存。
     */
    private fun loadPackageData(
        moduleKey: String,
        packageFqName: FqName,
        binaryFile: VirtualFile,
        rootFiles: List<VirtualFile>,
    ): LoadedCjoPackage? {
        val roots = rootFiles
            .map(::toRootFile)
            .map(::normalizeRoot)
            .distinctBy(File::getAbsolutePath)
        return repositoryFor(moduleKey, roots).loadPackageData(packageFqName, binaryFile, roots)
    }

    /**
     * 返回指定 module key 与搜索根对应的仓库实例。
     *
     * 缓存键包含 roots 列表，因此同一 module 在 binary roots 变化后会创建新的 [DecompiledCjoRepository]；
     * 旧缓存由 [refreshIfNeeded] 根据项目修改计数清理。
     */
    private fun repositoryFor(moduleKey: String, roots: List<File>): DecompiledCjoRepository {
        val key = RepositoryKey(moduleKey, roots)
        return repositories.computeIfAbsent(key) {
            DecompiledCjoRepository(it.roots)
        }
    }

    /**
     * 在项目结构发生变化时清空 `.cjo` 仓库缓存。
     *
     * 反编译索引依赖 module roots 与 builtins 列表，修改计数变化意味着既有 [RepositoryKey]
     * 可能已经不能代表当前项目结构。
     */
    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return
        repositories.clear()
        knownModificationCount = modificationCount
    }

    /**
     * 将 IntelliJ 虚拟文件转换为用于 [CjoSearchPath] 的物理文件路径。
     */
    private fun toRootFile(virtualFile: VirtualFile): File = File(virtualFile.path)

    /**
     * 将文件路径规范化为可作为 `.cjo` 搜索根的目录。
     *
     * 如果传入值本身是目录则直接使用；如果是具体 `.cjo` 文件，则使用其父目录，
     * 使 [CjoManager] 能按包名在该目录下定位对应 binary。
     */
    private fun normalizeRoot(file: File): File {
        return if (file.isDirectory) file else file.parentFile ?: file
    }
}
