package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirGlobalResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.serialization.provider.CjoPackageContextProvider
import org.cangnova.cangjie.cfir.serialization.cjo.distinctCjoSearchRoots
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.deserialization.LibraryPathFilter
import org.cangnova.cangjie.cfir.deserialization.MultipleModuleDataProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersionPolicy
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.source.CfirImportedSourceGraphProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import java.io.File

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
@OptIn(CaPlatformInterface::class, LLCfirInternals::class)
internal object LLBinaryOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    /**
     * binary-origin JVM library provider 当前复用 common library provider 创建路径。
     */
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        createCommonLibrarySymbolProvider(session, packagePartProvider, scope)

    /**
     * 创建基于 `.cjo` 反序列化的 common library symbol provider。
     */
    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        listOf(session.cjoSymbolProvider)

    /**
     * 创建 builtins session 使用的 `.cjo` 反序列化 provider 与 synthetic builtin provider。
     *
     * `.cjo` 中的真实 BuiltInDecl 必须优先于 synthetic 声明；后者只负责补缺。
     */
    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> =
        listOf(
            session.cjoSymbolProvider,
            CfirBuiltinSymbolProvider(session, session.moduleData),
        )

    /**
     * 为 STUBS 来源的 source session 提供 builtins 的二进制 extend 来源。
     *
     * stub 形态的 builtins provider 不携带 `CfirExtend`，`extend Int8 <: ToString` 等库 extend
     * 必须仍从 `.cjo` 反序列化获得，否则 primitive/`String` 的扩展成员在源码中不可见。
     */
    fun createBuiltinsExtendProvider(session: LLCfirSession): CfirSymbolProvider =
        session.cjoSymbolProvider

    /**
     * 为 STUBS 来源的 source session 提供库依赖的二进制 extend 来源。
     */
    fun createLibraryExtendProvider(session: LLCfirSession): CfirSymbolProvider =
        session.cjoSymbolProvider

    /** 两种 binary 读取入口均复用 session-owned provider；不借 session.symbolProvider 查找目标。 */
    internal fun createCjoSymbolProvider(session: LLCfirSession): CfirDeserializedSymbolProvider =
        if (session.caModule is CaBuiltinsModule) createBuiltinsDeserializedSymbolProvider(session)
        else createDeserializedLibrarySymbolProvider(session)

    /** 本模块只暴露 own roots；imports 先定位实际依赖模块的 own CJO 快照，再取其 context。 */
    private fun createDeserializedLibrarySymbolProvider(session: LLCfirSession): CfirDeserializedSymbolProvider {
        val libraryModule = session.caModule as? CaLibraryModule
        val explicitCommonPaths = libraryModule?.cjmpCommonPartCjoPaths.orEmpty()
        val librarySearchPaths = libraryModule?.binaryRoots.orEmpty()
            .mapNotNull { it.virtualFile }
            .mapNotNull(VirtualFile::asCjoSearchResource)
            .distinctCjoSearchRoots()
        // 初始图按实际 module identity 捕获；平台 canonical provider 不能抹掉另一个实际 owner。
        val dependencyModules = run {
            val modules = mutableListOf<CaModule>()
            val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CaModule, Boolean>())
            val pending = java.util.ArrayDeque<CaModule>()
            pending.addAll(session.caModule.allDirectDependencies)
            while (pending.isNotEmpty()) {
                val module = pending.removeFirst()
                if (module !== session.caModule && seen.add(module)) {
                    modules += module
                    pending.addAll(module.allDirectDependencies)
                }
            }
            // 二进制库图可缺显式依赖；snapshot 中的每个实际 builtin/library owner 都进入原图。
            for (module in org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
                .getInstance(session.project).snapshot.allModules) {
                if (module !== session.caModule &&
                    (module is CaBuiltinsModule || module is CaLibraryModule || module is CaLibraryFallbackDependenciesModule) &&
                    seen.add(module)) modules += module
            }
            modules.toList()
        }
        val dependencies by lazy {
            buildList<Pair<CaModule, CfirDeserializedSymbolProvider>> {
                val builtinsFactory = LLCfirBuiltinsSessionFactory.getInstance(session.project)
                val builtinModules = dependencyModules.filterIsInstance<CaBuiltinsModule>()
                    .filter { it.targetPlatform == session.caModule.targetPlatform }
                if (builtinModules.isEmpty()) {
                    // 原 implicit builtin 服务来自 consumer 的明确平台；没有实际候选时保留这一正式服务。
                    val builtinSession = builtinsFactory.getBuiltinsSession(session.caModule.targetPlatform)
                    add(builtinSession.caModule to builtinSession.cjoSymbolProvider)
                } else {
                    for (module in builtinModules) {
                        add(module to builtinsFactory.getBuiltinsSession(module.targetPlatform).cjoSymbolProvider)
                    }
                }
                val builtinsFiles = BuiltinsVirtualFileProvider.getInstance()
                for (module in dependencyModules) {
                    if (module is CaLibraryModule) {
                        val roots = module.binaryRoots.mapNotNull { it.virtualFile }
                        // SDK 展示库不重复认领 builtins 的同一实际文件；其它 roots 保留多 owner 检查。
                        if (roots.isNotEmpty() && builtinsFiles.areAllInBuiltinRoot(roots, session.project)) continue
                    }
                    if (module is CaLibraryModule || module is CaLibraryFallbackDependenciesModule) {
                        LLCfirSessionCache.getInstance(session.project).getDependencySession(module)
                            ?.let { add(module to it.cjoSymbolProvider) }
                    }
                }
            }
        }
        return CfirDeserializedSymbolProvider(
            session = session,
            cjoManager = CjoManager(
                CjoSearchPath.forRoots(builtinsRoots = emptyList(), libraryRoots = librarySearchPaths),
                CjoFormatVersionPolicy.DeferredToConsumerSession,
                explicitCommonPartPaths = explicitCommonPaths,
                declarationMaterialization = LLCfirGlobalResolveComponents.getInstance(session.project).cjoMaterialization,
            ),
            cangjieScopeProvider = session.cangjieScopeProvider,
            libraryModuleData = session.moduleData,
            moduleDataProvider = MultipleModuleDataProvider(
                mapOf(session.moduleData to LibraryPathFilter.LibraryPathsAndRoots(
                    explicitCommonPaths,
                    librarySearchPaths.map(File::toPath),
                )), session.moduleData,
            ),
            importedPackageContextProvider = CjoPackageContextProvider { name, mode ->
                // 仅检查 metadata 快照；缺失和多 owner 都不按名称或 consumer module 猜测。
                val targets = dependencies.mapNotNull { (module, provider) ->
                    provider.getOwnPackageContext(name, mode)?.let { module to it }
                }
                check(targets.size <= 1) { "Competing CJO module owners for " + name }
                targets.singleOrNull()?.second?.also { target ->
                    LLCfirGlobalResolveComponents.getInstance(session.project).registerCjoDependency(
                        session, (target.moduleData as org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData).caModule,
                    )
                }
            },
        ).also { session.register(CfirImportedSourceGraphProvider::class, it) }
    }

    /**
     * 仓颉的 builtins session 需要同时覆盖两类符号：
     * 1. 真正的 primitive / builtin provider；
     * 2. `std.core` 等 stdlib `.cjo` 中定义的核心类型（例如 `String`）。
     *
     * Kotlin 的 binary-origin builtins session 只保留 builtins provider 即可，
     * 但仓颉的 `String` 不属于 primitive builtins，因此这里必须额外接入
     * builtins `.cjo` 搜索根对应的反序列化 provider。
     */
    private fun createBuiltinsDeserializedSymbolProvider(session: LLCfirSession): CfirDeserializedSymbolProvider {
        // 搜索根显式来自 builtins provider，不再回退到 CANGJIE_STDLIB_MODULE / CANGJIE_LIBRARY 环境变量：
        // IDE 里 SDK 由项目设置决定，按环境变量解析会查到另一份 SDK 的目录。
        // 原文件 provider 的实际清单就是 own scope；单文件不能折叠为可认领 sibling 的父目录。
        val builtinsRoots = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(session.project)
            .map { File(it.path) }
            .distinctCjoSearchRoots()

        /*
         * builtins `.cjo` roots 为空是“静默失明”的源头：builtins 的 extend 元数据（如
         * `extend Array<T> <: Collection<T> { prop size }`）全部来自 `.cjo` 反序列化，
         * roots 为空时 `Array` 仍可能由 `CfirBuiltinSymbolProvider` 合成（无成员），
         * 表现就是 `'size' is not a member of 'Array'` 这类“类型在、成员不在”。
         * 这里只告警一次（按 project 去重），不改变行为、不做兜底。
         */
        if (builtinsRoots.isEmpty()) {
            BuiltinsRootsDiagnostics.warnOnce(session.project)
        }

        return CfirDeserializedSymbolProvider(
            session = session,
            cjoManager = CjoManager(
                CjoSearchPath.forRoots(builtinsRoots = emptyList(), libraryRoots = builtinsRoots),
                CjoFormatVersionPolicy.Compilation(session.languageVersionSettings),
                declarationMaterialization = LLCfirGlobalResolveComponents.getInstance(session.project).cjoMaterialization,
            ),
            cangjieScopeProvider = session.cangjieScopeProvider,
            libraryModuleData = session.moduleData,
            moduleDataProvider = MultipleModuleDataProvider(
                mapOf(session.moduleData to LibraryPathFilter.LibraryList(
                    builtinsRoots.mapTo(linkedSetOf()) { it.toPath() },
                )), session.moduleData,
            ),
        ).also { session.register(CfirImportedSourceGraphProvider::class, it) }
    }
}

/** LL 保留 binary root 的实际文件/目录身份；格式路径规范化由 serialization 基础层承担。 */
private fun VirtualFile.asCjoSearchResource(): File? =
    File(path).absoluteFile.normalize().takeIf {
        it.isDirectory || it.isFile && it.extension.equals("cjo", ignoreCase = true)
    }

/**
 * builtins `.cjo` roots 为空的诊断（每个 project 只告警一次）。
 *
 * 不改行为，只把“静默失明”变成 idea.log 里可见的一条 warn，便于现场定位
 * `'size' is not a member of 'Array'` 这类由 extend 元数据缺失引起的问题。
 */
private object BuiltinsRootsDiagnostics {
    private val log = com.intellij.openapi.diagnostic.logger<LLBinaryOriginLibrarySymbolProviderFactory>()

    /**
     * 已告警的 project（弱引用，避免 IDE 长期运行/多 project 时泄漏）。
     */
    private val warned: MutableSet<com.intellij.openapi.project.Project> =
        java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(java.util.WeakHashMap()))

    fun warnOnce(project: com.intellij.openapi.project.Project) {
        if (!warned.add(project)) return
        log.warn(
            "Cangjie builtins .cjo roots are empty; std.core extend metadata " +
                "(e.g. extend Array<T> <: Collection<T> { prop size }) will be invisible. " +
                "Check the project SDK stdlib path (CjSdk.stdlibPath / <home>/modules).",
        )
    }
}
