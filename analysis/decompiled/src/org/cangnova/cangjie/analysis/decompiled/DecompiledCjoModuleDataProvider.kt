@file:OptIn(
    org.cangnova.cangjie.analysis.api.CaPlatformInterface::class,
    org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals::class,
)

package org.cangnova.cangjie.analysis.decompiled

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.modification.CaModificationTracker
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureSnapshot
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoModuleDataProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.DetachedCjoModuleData
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.cfir.serialization.cjd.CjdSidecarLocator
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider
import org.cangnova.cangjie.cfir.session.DetachedCjoSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.name.FqName
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap

/**
 * actual-file reader 的 owner 服务。结构内使用真实 LL session 的原 named metadata producer；
 * 结构外每个实际文件的一代 module/manager/binding 由本服务统一装配、发布和缓存。
 */
class DecompiledCjoModuleDataProvider(private val project: Project) : CjoModuleDataProvider {
    private val detachedProducers = ConcurrentHashMap<Path, DetachedProducer>()

    /** owner-only 与内容读取接口共用同一原 producer，不能分别创建 detached module。 */
    override fun getModuleData(binaryFile: VirtualFile): CfirModuleData =
        getPackageContext(binaryFile, binaryFile.contentsToByteArray(false), CjoDeserializationMode.SIGNATURE_ONLY).moduleData

    override fun getPackageContext(
        binaryFile: VirtualFile,
        content: ByteArray,
        mode: CjoDeserializationMode,
    ): CfirDeserializationContext {
        val owner = CaDecompiledBinaryIndex.getInstance(project).findOwningModule(binaryFile)
        return if (owner == null) {
            detachedProducer(binaryFile, content).context(content, mode)
        } else {
            // 本代项目结构已给出真实 owner，退休此前该文件的 detached 装配。
            detachedProducers.remove(sourcePath(binaryFile))
            contextForOwner(binaryFile, content, mode, owner)
        }
    }

    /** 已确定的 module/group 不再被无上下文 owner 查询替换；实际文件仍必须属于该模块。 */
    override fun getPackageContext(
        binaryFile: VirtualFile,
        content: ByteArray,
        mode: CjoDeserializationMode,
        ownerModule: CaModule,
    ): CfirDeserializationContext {
        val index = CaDecompiledBinaryIndex.getInstance(project)
        val files = when (ownerModule) {
            is CaBuiltinsModule -> index.getBinaryFiles(ownerModule)
            is CaLibraryModule -> index.getBinaryFiles(ownerModule)
            else -> error("Actual CJO reader requires a binary owner module")
        }
        check(files.any { it.url == binaryFile.url }) { "CJO source is outside the requested binary module" }
        return contextForOwner(binaryFile, content, mode, ownerModule)
    }

    private fun contextForOwner(
        binaryFile: VirtualFile, content: ByteArray, mode: CjoDeserializationMode, owner: CaModule,
    ): CfirDeserializationContext {
        val session = when (owner) {
            is CaBuiltinsModule -> {
                requireUniqueBuiltinSourceOwner(binaryFile, owner)
                LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(owner.targetPlatform)
            }
            is CaLibraryModule -> LLCfirSessionCache.getInstance(project).getSession(owner, preferBinary = true)
            else -> error("Actual CJO reader requires a binary owner module")
        }
        val name = packageName(binaryFile, content)
        return session.getOwnCjoPackageContext(name.asString(), sourcePath(binaryFile), content, mode).also {
            check(it.moduleData === session.moduleData) { "CJO content and file have different module owners" }
        }
    }

    /** 显式平台选择仍须先保留实际模块竞争；单个 host owner 可以使用 factory 的 canonical module。 */
    private fun requireUniqueBuiltinSourceOwner(binaryFile: VirtualFile, owner: CaBuiltinsModule) {
        val index = CaDecompiledBinaryIndex.getInstance(project)
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CaBuiltinsModule, Boolean>())
        val candidates = CaModuleProvider.getInstance(project).snapshot.allModules
            .filterIsInstance<CaBuiltinsModule>()
            .filter { seen.add(it) && it.targetPlatform == owner.targetPlatform }
            .filter { module -> index.getBinaryFiles(module).any { it.url == binaryFile.url } }
        check(candidates.size <= 1) {
            "Competing CJO builtin module owners for " + binaryFile.url + " on " + owner.targetPlatform
        }
    }

    /** 每个来源原子选择并发布一个完整 producer；同代并发读取只复用已有装配。 */
    private fun detachedProducer(binaryFile: VirtualFile, content: ByteArray): DetachedProducer {
        val path = sourcePath(binaryFile)
        val generation = generation(binaryFile, path)
        return checkNotNull(detachedProducers.compute(path) { _, previous ->
            if (previous != null && previous.generation == generation && previous.dependenciesAreValid) previous
            else DetachedProducer(binaryFile, path, generation, content)
        })
    }

    private fun sourcePath(binaryFile: VirtualFile): Path {
        check(binaryFile.fileSystem.protocol == "file") {
            "Actual CJO source requires a physical file; memory projection must supply its own context"
        }
        return Paths.get(binaryFile.path).toAbsolutePath().normalize()
    }

    private fun packageName(binaryFile: VirtualFile, content: ByteArray): FqName =
        checkNotNull(CjoBinaryFileReader.readPackageFqName(binaryFile, content)) { "CJO content has no package name" }

    /**
     * detached generation 保留实际模块快照、roots 与来源/sidecar 版本。宿主提供的 module tracker
     * 参与同一代身份；未注册时不虚构 owner/平台，来源和显式模块资源身份仍须一致。
     */
    private fun generation(binaryFile: VirtualFile, path: Path): DetachedGeneration {
        val structure = CaModuleProvider.getInstance(project).snapshot
        val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CaModule, Boolean>())
        val modules = structure.allModules.filter(seen::add)
        val builtins = modules.filterIsInstance<CaBuiltinsModule>()
        val builtinFiles = if (builtins.isEmpty()) emptySet()
            else BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(project)
        val builtinRoots = builtinFiles.mapTo(sortedSetOf()) { it.parent?.url ?: it.url }.toList()
        val owners = buildList {
            builtins.forEach { add(OwnerResource(it, builtinRoots)) }
            modules.filterIsInstance<CaLibraryModule>().forEach { module ->
                val roots = module.binaryRoots.mapNotNull { it.virtualFile }
                // SDK 展示库与真实 builtins 共享 roots；只在本代装配时判定，不在 import 读端猜 owner。
                if (roots.isNotEmpty() && builtins.isNotEmpty() &&
                    BuiltinsVirtualFileProvider.getInstance().areAllInBuiltinRoot(roots, project)) return@forEach
                add(OwnerResource(module, roots.map { it.url }))
            }
        }
        val sidecar = CjdSidecarLocator.findReadable(path)
        return DetachedGeneration(
            structure, CaModificationTracker.getInstance(project)?.modificationCount,
            VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS.modificationCount,
            binaryFile.modificationStamp, fileVersion(path), sidecar?.let(::fileVersion), owners,
        )
    }

    private fun fileVersion(path: Path): SourceVersion {
        val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
        check(attributes.isRegularFile) { "CJO metadata source is not a regular file: " + path }
        return SourceVersion(path, attributes.fileKey(), attributes.lastModifiedTime(), attributes.size())
    }

    private data class SourceVersion(
        val path: Path, val fileKey: Any?, val modified: FileTime, val size: Long,
    )

    /** 根值固定，module 使用原对象 identity；同名不同图的模块不能合并。 */
    private class OwnerResource(val module: CaModule, val roots: List<String>) {
        override fun equals(other: Any?): Boolean = other is OwnerResource && module === other.module && roots == other.roots
        override fun hashCode(): Int = 31 * System.identityHashCode(module) + roots.hashCode()
    }

    private data class DetachedGeneration(
        val structure: CaProjectStructureSnapshot,
        val moduleModificationCount: Long?,
        val vfsStructureModificationCount: Long,
        val fileModificationStamp: Long,
        val source: SourceVersion,
        val sidecar: SourceVersion?,
        val owners: List<OwnerResource>,
    )

    /** 导入服务、path owner 与原 named producer 随单文件代一起重建，绝不更新旧 binding 的默认表。 */
    private inner class DetachedProducer(
        private val binaryFile: VirtualFile,
        private val path: Path,
        val generation: DetachedGeneration,
        content: ByteArray,
    ) {
        private val name = packageName(binaryFile, content)
        val moduleData = DetachedCjoModuleData(name, DetachedCjoSession(name,
            CangJieProjectStructureProvider.getInstance(project).libraryLanguageVersionSettings))
        private val dependencySessions = lazy {
            check(generation == generation(binaryFile, path)) { "Detached CJO metadata generation changed" }
            generation.owners.map { owner ->
                owner.module to when (val module = owner.module) {
                    is CaBuiltinsModule -> LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(module.targetPlatform)
                    is CaLibraryModule -> LLCfirSessionCache.getInstance(project).getSession(module, preferBinary = true)
                    else -> error("Detached CJO import has no binary module owner")
                }
            }
        }
        val dependenciesAreValid: Boolean
            get() = !dependencySessions.isInitialized() || dependencySessions.value.all { it.second.isValid }

        private val provider = CfirDeserializedSymbolProvider(
            session = moduleData.session,
            cjoManager = LLCfirSession.createCjoDeclarationManager(project,
                CjoSearchPath.forRoots(emptyList(), listOf(path.toFile()))),
            cangjieScopeProvider = moduleData.session.cangjieScopeProvider,
            libraryModuleData = moduleData,
            moduleDataProvider = MultipleModuleDataProvider(
                mapOf(moduleData to LibraryPathFilter.LibraryList(setOf(path))), moduleData,
            ),
            importedPackageContextProvider = CjoPackageContextProvider { targetName, mode ->
                check(generation == generation(binaryFile, path) && dependenciesAreValid) {
                    "Detached CJO imports require their original valid metadata generation"
                }
                val sessions = dependencySessions.value
                check(sessions.all { it.second.isValid }) { "Detached CJO import session is invalid" }
                // owner 候选保持实际 module 身份；同平台的两份模块不能因 factory 共用 session 而先合并。
                val targets = sessions.mapNotNull { (module, session) ->
                    session.getOwnCjoPackageContext(targetName, mode)?.let { module to it }
                }
                check(targets.size <= 1) { "Multiple CJO module owners for " + targetName }
                targets.singleOrNull()?.second
            },
        )

        init {
            val actual = provider.getOwnPackageContext(name.asString(), path, content, CjoDeserializationMode.SIGNATURE_ONLY)
            val named = checkNotNull(provider.getOwnPackageContext(name.asString(), CjoDeserializationMode.SIGNATURE_ONLY))
            check(named === actual) { "Detached named/actual readers have different source snapshots" }
            check(generation == generation(binaryFile, path)) { "CJO source changed while publishing its detached producer" }
        }

        fun context(content: ByteArray, mode: CjoDeserializationMode): CfirDeserializationContext {
            check(generation == generation(binaryFile, path) && dependenciesAreValid) {
                "Detached CJO reader requires its original valid metadata generation"
            }
            return provider.getOwnPackageContext(name.asString(), path, content, mode)
        }
    }
}
