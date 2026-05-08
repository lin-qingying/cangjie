@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.decompiled.filestubs

import PackageFormat.Package as CjoPackage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsRootAware
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.name.FqName
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * `.cjo` binary file 视图的统一底座。
 *
 * 它对上游暴露的是稳定的二进制身份与包加载入口，
 * 把模块根扫描、builtins 发现、package 定位与 `CjoManager` 缓存收敛到一起。
 */
class CaDecompiledBinarySupport(
    private val project: Project,
) {
    @Volatile
    private var knownModificationCount: Long = Long.MIN_VALUE

    private val libraryIndexes = ConcurrentHashMap<String, CaModuleBinaryIndex>()
    private var builtinsIndex: CaModuleBinaryIndex? = null
    private val repositories = ConcurrentHashMap<CaRepositoryKey, CaDecompiledCjoRepository>()

    fun getBinaryFiles(module: CaLibraryModule): List<VirtualFile> {
        refreshIfNeeded()
        return indexFor(module).files
    }

    fun getBinaryFiles(module: CaBuiltinsModule): List<VirtualFile> {
        refreshIfNeeded()
        return builtinsIndex().files
    }

    fun findBinaryFile(module: CaLibraryModule, packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return indexFor(module).packageFiles[packageFqName]
    }

    fun findBinaryFile(module: CaBuiltinsModule, packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return builtinsIndex().packageFiles[packageFqName]
    }

    fun findBuiltinsBinaryFile(packageFqName: FqName): VirtualFile? {
        refreshIfNeeded()
        return builtinsIndex().packageFiles[packageFqName]
    }

    fun findOwningModule(binaryFile: VirtualFile): CaModule? {
        refreshIfNeeded()
        if (builtinsIndex().files.any { it.url == binaryFile.url }) {
            val decompiledFile = project.getService(CaDecompiledPsiProvider::class.java)?.getDecompiledFile(binaryFile) ?: return null
            return CangJieProjectStructureProvider.getModule(project, decompiledFile, useSiteModule = null)
        }

        val projectStructure = CaModuleProvider.getInstance(project)

        /**
         * library 侧也必须与 builtins 一样按“已建索引的精确 binary file”来反查 owning module。
         *
         * 不能只按 root 祖先关系判断，否则多个 library module 共享目录、目录嵌套，
         * 或者某个 root 下存在并不属于当前 binary index 的文件时，都会把归属放大成“目录命中”。
         * decompiled 框架这里需要的是稳定的一对一 binary 身份，而不是宽松的路径包含关系。
         */
        projectStructure.allModules.filterIsInstance<CaLibraryModule>()
            .firstOrNull { module -> indexFor(module).files.any { it.url == binaryFile.url } }
            ?.let { return it }

        return null
    }

    fun readPackageFqName(binaryFile: VirtualFile): FqName? {
        return CaCjoBinaryFileReader.readPackageFqName(binaryFile)
    }

    fun loadPackageData(binaryFile: VirtualFile): CaLoadedCjoPackage? {
        refreshIfNeeded()
        val packageFqName = readPackageFqName(binaryFile) ?: return null
        if (builtinsIndex().files.any { it.url == binaryFile.url }) {
            return loadBuiltinsPackageData(packageFqName, binaryFile)
        }

        val module = findOwningModule(binaryFile) ?: return null
        return when (module) {
            is CaLibraryModule -> loadPackageData(module, packageFqName)
            is CaBuiltinsModule -> loadPackageData(module, packageFqName)
            else -> null
        }
    }

    fun loadPackageData(module: CaLibraryModule, packageFqName: FqName): CaLoadedCjoPackage? {
        refreshIfNeeded()
        val binaryFile = findBinaryFile(module, packageFqName) ?: return null
        val roots = repositoryRoots(module)
        return repositoryFor(moduleKey(module), roots).loadPackageData(packageFqName, binaryFile, module, roots)
    }

    fun loadPackageData(module: CaBuiltinsModule, packageFqName: FqName): CaLoadedCjoPackage? {
        refreshIfNeeded()
        val binaryFile = findBinaryFile(module, packageFqName) ?: return null
        return loadBuiltinsPackageData(packageFqName, binaryFile, module)
    }

    private fun loadBuiltinsPackageData(
        packageFqName: FqName,
        binaryFile: VirtualFile,
        owningModule: CaModule? = null,
    ): CaLoadedCjoPackage? {
        val roots = builtinsRootFiles().map(::toRootFile).map(::normalizeRoot).distinctBy(File::getAbsolutePath)
        return repositoryFor("<builtins>", roots).loadPackageData(packageFqName, binaryFile, owningModule, roots)
    }

    private fun indexFor(module: CaLibraryModule): CaModuleBinaryIndex {
        val key = moduleKey(module)
        return libraryIndexes.computeIfAbsent(key) {
            buildIndex(collectBinaryFiles(module.binaryRoots))
        }
    }

    private fun builtinsIndex(): CaModuleBinaryIndex {
        builtinsIndex?.let { return it }
        return buildIndex(CaBuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles().toList())
            .also { builtinsIndex = it }
    }

    private fun buildIndex(files: List<VirtualFile>): CaModuleBinaryIndex {
        val packageFiles = linkedMapOf<FqName, VirtualFile>()
        files.forEach { file ->
            val packageFqName = readPackageFqName(file) ?: return@forEach
            packageFiles.putIfAbsent(packageFqName, file)
        }
        return CaModuleBinaryIndex(packageFiles.values.toList(), packageFiles)
    }

    private fun collectBinaryFiles(items: List<PsiFileSystemItem>): List<VirtualFile> {
        val files = linkedSetOf<VirtualFile>()
        items.forEach { item ->
            when (item) {
                is PsiDirectory -> {
                    VfsUtilCore.iterateChildrenRecursively(item.virtualFile, null) { child ->
                        if (!child.isDirectory && isBuiltInBinary(child)) {
                            files += child
                        }
                        true
                    }
                }

                is PsiFile -> {
                    val virtualFile = item.virtualFile ?: return@forEach
                    if (isBuiltInBinary(virtualFile)) {
                        files += virtualFile
                    }
                }
            }
        }
        return files.toList()
    }

    private fun moduleKey(module: CaLibraryModule): String {
        return module.stableModuleName ?: module.moduleDescription
    }

    private fun repositoryRoots(module: CaLibraryModule): List<File> {
        return module.binaryRoots
            .mapNotNull { it.virtualFile }
            .map(::toRootFile)
            .map(::normalizeRoot)
            .distinctBy(File::getAbsolutePath)
    }

    private fun builtinsRootFiles(): Set<VirtualFile> {
        val provider = CaBuiltinsVirtualFileProvider.getInstance()
        return when (provider) {
            is CaBuiltinsRootAware -> provider.getBuiltinRootVirtualFiles()
            else -> error(
                "Builtins decompiled support requires `${CaBuiltinsRootAware::class.simpleName}` to recover `.cjo` repository roots; " +
                    "provider=${provider::class.qualifiedName}",
            )
        }
    }

    private fun repositoryFor(moduleKey: String, roots: List<File>): CaDecompiledCjoRepository {
        val key = CaRepositoryKey(moduleKey, roots)
        return repositories.computeIfAbsent(key) {
            CaDecompiledCjoRepository(it.roots)
        }
    }

    private fun refreshIfNeeded() {
        val modificationCount = project.getService(CaModificationTracker::class.java)?.modificationCount ?: 0L
        if (knownModificationCount == modificationCount) return

        libraryIndexes.clear()
        builtinsIndex = null
        repositories.clear()
        knownModificationCount = modificationCount
    }

    private fun isBuiltInBinary(file: VirtualFile): Boolean = CaCjoBinaryFileReader.isCjoBinaryFile(file)

    private fun toRootFile(virtualFile: VirtualFile): File = File(virtualFile.path)

    private fun normalizeRoot(file: File): File {
        return if (file.isDirectory) file else file.parentFile ?: file
    }
}

data class CaLoadedCjoPackage(
    val owningModule: CaModule?,
    val binaryFile: VirtualFile,
    val packageFqName: FqName,
    val pkg: CjoPackage,
    val header: CjoPackageHeader,
    val searchRoots: List<File>,
    val isVersionSupported: Boolean,
)

data class CaModuleBinaryIndex(
    val files: List<VirtualFile>,
    val packageFiles: Map<FqName, VirtualFile>,
)

internal data class CaRepositoryKey(
    val moduleKey: String,
    val roots: List<File>,
)

internal class CaDecompiledCjoRepository(
    roots: List<File>,
) {
    private val rootPathString = roots.joinToString(File.pathSeparator) { it.absolutePath }
    private val cjoManager = CjoManager(
        CjoSearchPath { key ->
            when (key) {
                "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> rootPathString
                else -> null
            }
        },
    )

    fun loadPackageData(
        packageFqName: FqName,
        binaryFile: VirtualFile,
        owningModule: CaModule?,
        searchRoots: List<File>,
    ): CaLoadedCjoPackage? {
        val fullPkgName = packageFqName.asString()
        val pkg = cjoManager.loadPackage(fullPkgName) ?: return null
        val header = cjoManager.loadPackageHeader(fullPkgName) ?: return null
        return CaLoadedCjoPackage(
            owningModule = owningModule,
            binaryFile = binaryFile,
            packageFqName = packageFqName,
            pkg = pkg,
            header = header,
            searchRoots = searchRoots,
            isVersionSupported = CaCjoBinaryFileReader.isSupportedVersion(pkg),
        )
    }
}
