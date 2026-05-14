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
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` package 数据查找器。
 *
 * 只负责按 binary / module / package 恢复反编译所需的 package 数据，
 * 不再承担 binary 索引职责。
 */
class DecompiledPackageDataFinder(
    private val project: Project,
) {
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    private val repositories = ConcurrentHashMap<RepositoryKey, DecompiledCjoRepository>()

    private val binaryIndex: CaDecompiledBinaryIndex
        get() = project.getService(CaDecompiledBinaryIndex::class.java)

    fun loadPackageData(binaryFile: VirtualFile): LoadedCjoPackage? {
        refreshIfNeeded()
        val packageFqName = binaryIndex.readPackageFqName(binaryFile) ?: return null
        val module = binaryIndex.findOwningModule(binaryFile) ?: return null
        return when (module) {
            is CaLibraryModule -> loadPackageData(module, packageFqName)
            is CaBuiltinsModule -> loadPackageData(packageFqName, binaryFile, listOf(binaryFile))
            else -> null
        }
    }

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

    fun loadPackageData(module: CaBuiltinsModule, packageFqName: FqName): LoadedCjoPackage? {
        refreshIfNeeded()
        val binaryFile = binaryIndex.findBinaryFile(module, packageFqName) ?: return null
        return loadPackageData(packageFqName, binaryFile, listOf(binaryFile))
    }

    private fun loadPackageData(
        packageFqName: FqName,
        binaryFile: VirtualFile,
        rootFiles: List<VirtualFile>,
    ): LoadedCjoPackage? {
        val roots = rootFiles
            .map(::toRootFile)
            .map(::normalizeRoot)
            .distinctBy(File::getAbsolutePath)
        return repositoryFor("<builtins>", roots).loadPackageData(packageFqName, binaryFile, roots)
    }

    private fun repositoryFor(moduleKey: String, roots: List<File>): DecompiledCjoRepository {
        val key = RepositoryKey(moduleKey, roots)
        return repositories.computeIfAbsent(key) {
            DecompiledCjoRepository(it.roots)
        }
    }

    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return
        repositories.clear()
        knownModificationCount = modificationCount
    }

    private fun toRootFile(virtualFile: VirtualFile): File = File(virtualFile.path)

    private fun normalizeRoot(file: File): File {
        return if (file.isDirectory) file else file.parentFile ?: file
    }
}
