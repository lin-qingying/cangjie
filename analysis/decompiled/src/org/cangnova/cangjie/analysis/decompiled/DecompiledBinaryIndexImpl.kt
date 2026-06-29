@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.TargetPlatform
import java.util.concurrent.ConcurrentHashMap

/**
 * 对外 `.cjo` binary index facade。
 *
 * 根模块只负责装配公开服务，
 * 不再承载反序列化与 PSI 构造实现。
 */
class DecompiledBinaryIndexImpl(
    /**
     * 提供项目结构、修改计数、builtins session factory 等服务的 IntelliJ project。
     */
    private val project: Project,
) : CaDecompiledBinaryIndex {
    /**
     * 上次同步 binary index 缓存时观察到的项目结构修改计数。
     */
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    /**
     * Library module 对应的 `.cjo` binary index 缓存。
     */
    private val libraryIndexes = ConcurrentHashMap<String, ModuleBinaryIndex>()

    /**
     * Builtins 目标平台对应的 `.cjo` binary index 缓存。
     */
    private val builtinsIndexes = ConcurrentHashMap<TargetPlatform, ModuleBinaryIndex>()

    /**
     * 返回指定 library module 可见的 `.cjo` 二进制文件列表。
     */
    override fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile> {
        refreshIfNeeded()
        return indexFor(module).files
    }

    /**
     * 返回指定 builtins module 可见的 `.cjo` 二进制文件列表。
     */
    override fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile> {
        refreshIfNeeded()
        return builtinsIndex(module.targetPlatform).files
    }

    /**
     * 从 `.cjo` 二进制头部读取包全限定名。
     */
    override fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        return CjoBinaryFileReader.readPackageFqName(binaryFile)
    }

    /**
     * 在指定 library module 的 binary roots 中查找承载目标包的 `.cjo` 文件。
     */
    override fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return indexFor(module).packageFiles[packageFqName]
    }

    /**
     * 在指定 builtins module 中查找承载目标包的 `.cjo` 文件。
     */
    override fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return builtinsIndex(module.targetPlatform).packageFiles[packageFqName]
    }

    /**
     * 在当前项目已知 builtins modules 和默认平台 builtins 中查找目标包的 `.cjo` 文件。
     */
    override fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        val projectStructure = CaModuleProvider.getInstance(project)
        knownBuiltinsModules(projectStructure).forEach { module ->
            builtinsIndex(module.targetPlatform).packageFiles[packageFqName]?.let { return it }
        }
        return builtinsIndex(CangJiePlatforms.defaultCangJiePlatform).packageFiles[packageFqName]
    }

    /**
     * 查找指定 `.cjo` 文件归属的分析模块。
     *
     * Builtins 优先按项目结构中的目标平台匹配；无法确认平台时回退到默认平台 builtins module；
     * library 文件再按所有 library module 的 binary index 搜索。
     */
    override fun findOwningModule(binaryFile: VirtualFile): CaModule? {
        refreshIfNeeded()
        val projectStructure = CaModuleProvider.getInstance(project)
        knownBuiltinsModules(projectStructure).firstOrNull { module ->
            builtinsIndex(module.targetPlatform).files.any { it.url == binaryFile.url }
        }?.let { return it }

        if (builtinsIndex(CangJiePlatforms.defaultCangJiePlatform).files.any { it.url == binaryFile.url }) {
            // 当前 builtins 二进制索引本身不携带高层 targetPlatform 身份，因此这里只能退回默认平台。
            // 一旦 decompiled/index 层能区分 builtins 文件归属的平台，再把这里接成真实 targetPlatform。
            return LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsModule(CangJiePlatforms.defaultCangJiePlatform)
        }

        projectStructure.allModules.filterIsInstance<CaLibraryModule>()
            .firstOrNull { module -> indexFor(module).files.any { it.url == binaryFile.url } }
            ?.let { return it }

        return null
    }

    /**
     * 返回或构建指定 library module 的 binary index。
     */
    private fun indexFor(module: CaLibraryModule): ModuleBinaryIndex {
        val key = module.stableModuleName ?: module.moduleDescription
        return libraryIndexes.computeIfAbsent(key) {
            buildIndex(collectBinaryFiles(module.binaryRoots))
        }
    }

    /**
     * 返回或构建指定目标平台的 builtins binary index。
     */
    private fun builtinsIndex(targetPlatform: TargetPlatform): ModuleBinaryIndex =
        builtinsIndexes.computeIfAbsent(targetPlatform) {
            buildIndex(BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(project).toList())
        }

    /**
     * 返回项目结构中按目标平台去重后的 builtins modules。
     */
    private fun knownBuiltinsModules(projectStructure: CaModuleProvider): Sequence<CaBuiltinsModule> =
        projectStructure.allModules
            .asSequence()
            .filterIsInstance<CaBuiltinsModule>()
            .distinctBy { module -> module.targetPlatform }

    /**
     * 根据 `.cjo` 文件集合构建包名到文件的索引。
     *
     * 同一包名出现多次时保留第一个文件，保持索引结果稳定。
     */
    private fun buildIndex(files: List<VirtualFile>): ModuleBinaryIndex {
        val packageFiles = linkedMapOf<FqName, VirtualFile>()
        files.forEach { file ->
            val packageFqName = readPackageFqName(file) ?: return@forEach
            packageFiles.putIfAbsent(packageFqName, file)
        }
        return ModuleBinaryIndex(packageFiles.values.toList(), packageFiles)
    }

    /**
     * 从 PSI 文件系统项集合中递归收集 `.cjo` 二进制文件。
     */
    private fun collectBinaryFiles(items: List<PsiFileSystemItem>): List<VirtualFile> {
        val files = linkedSetOf<VirtualFile>()
        items.forEach { item ->
            when (item) {
                is PsiDirectory -> {
                    VfsUtilCore.iterateChildrenRecursively(item.virtualFile, null) { child ->
                        if (!child.isDirectory && CjoBinaryFileReader.isCjoBinaryFile(child)) {
                            files += child
                        }
                        true
                    }
                }

                is PsiFile -> {
                    val virtualFile = item.virtualFile ?: return@forEach
                    if (CjoBinaryFileReader.isCjoBinaryFile(virtualFile)) {
                        files += virtualFile
                    }
                }
            }
        }
        return files.toList()
    }

    /**
     * 项目结构发生变化时清理 library 与 builtins binary index 缓存。
     */
    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return
        libraryIndexes.clear()
        builtinsIndexes.clear()
        knownModificationCount = modificationCount
    }
}

/**
 * 单个 module 或 builtins 平台的 `.cjo` binary index 快照。
 */
internal data class ModuleBinaryIndex(
    /** 参与索引的 `.cjo` 文件列表，顺序与包名索引中首次出现顺序一致。 */
    val files: List<VirtualFile>,

    /** 包全限定名到承载该包的 `.cjo` 文件的映射。 */
    val packageFiles: Map<FqName, VirtualFile>,
)
