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
    private val project: Project,
) : CaDecompiledBinaryIndex {
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    private val libraryIndexes = ConcurrentHashMap<String, ModuleBinaryIndex>()
    private val builtinsIndexes = ConcurrentHashMap<TargetPlatform, ModuleBinaryIndex>()

    override fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile> {
        refreshIfNeeded()
        return indexFor(module).files
    }

    override fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile> {
        refreshIfNeeded()
        return builtinsIndex(module.targetPlatform).files
    }

    override fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        return CjoBinaryFileReader.readPackageFqName(binaryFile)
    }

    override fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return indexFor(module).packageFiles[packageFqName]
    }

    override fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return builtinsIndex(module.targetPlatform).packageFiles[packageFqName]
    }

    override fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        val projectStructure = CaModuleProvider.getInstance(project)
        knownBuiltinsModules(projectStructure).forEach { module ->
            builtinsIndex(module.targetPlatform).packageFiles[packageFqName]?.let { return it }
        }
        return builtinsIndex(CangJiePlatforms.defaultCangJiePlatform).packageFiles[packageFqName]
    }

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

    private fun indexFor(module: CaLibraryModule): ModuleBinaryIndex {
        val key = module.stableModuleName ?: module.moduleDescription
        return libraryIndexes.computeIfAbsent(key) {
            buildIndex(collectBinaryFiles(module.binaryRoots))
        }
    }

    private fun builtinsIndex(targetPlatform: TargetPlatform): ModuleBinaryIndex =
        builtinsIndexes.computeIfAbsent(targetPlatform) {
            buildIndex(BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(project).toList())
        }

    private fun knownBuiltinsModules(projectStructure: CaModuleProvider): Sequence<CaBuiltinsModule> =
        projectStructure.allModules
            .asSequence()
            .filterIsInstance<CaBuiltinsModule>()
            .distinctBy { module -> module.targetPlatform }

    private fun buildIndex(files: List<VirtualFile>): ModuleBinaryIndex {
        val packageFiles = linkedMapOf<FqName, VirtualFile>()
        files.forEach { file ->
            val packageFqName = readPackageFqName(file) ?: return@forEach
            packageFiles.putIfAbsent(packageFqName, file)
        }
        return ModuleBinaryIndex(packageFiles.values.toList(), packageFiles)
    }

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

    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return
        libraryIndexes.clear()
        builtinsIndexes.clear()
        knownModificationCount = modificationCount
    }
}

internal data class ModuleBinaryIndex(
    val files: List<VirtualFile>,
    val packageFiles: Map<FqName, VirtualFile>,
)
