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
    private var builtinsIndex: ModuleBinaryIndex? = null

    override fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile> {
        refreshIfNeeded()
        return indexFor(module).files
    }

    override fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile> {
        refreshIfNeeded()
        return builtinsIndex().files
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
        return builtinsIndex().packageFiles[packageFqName]
    }

    override fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return builtinsIndex().packageFiles[packageFqName]
    }

    override fun findOwningModule(binaryFile: VirtualFile): CaModule? {
        refreshIfNeeded()
        if (builtinsIndex().files.any { it.url == binaryFile.url }) {
            return LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsModule()
        }

        val projectStructure = CaModuleProvider.getInstance(project)
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

    private fun builtinsIndex(): ModuleBinaryIndex {
        builtinsIndex?.let { return it }
        return buildIndex(BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().toList())
            .also { builtinsIndex = it }
    }

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
        builtinsIndex = null
        knownModificationCount = modificationCount
    }
}

internal data class ModuleBinaryIndex(
    val files: List<VirtualFile>,
    val packageFiles: Map<FqName, VirtualFile>,
)
