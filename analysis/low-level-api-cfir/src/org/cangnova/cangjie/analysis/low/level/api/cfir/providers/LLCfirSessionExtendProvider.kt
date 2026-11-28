package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.util.PsiModificationTracker
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.materializeTopLevelExtendFiles
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirCjmpExtendKey
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.createFileLookupScopes
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.cfir.session.cjmpOriginalExtendKey
import org.cangnova.cangjie.cfir.session.sameKeyAs
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Low-level source session 的 extend provider。
 *
 * 主编译器在 `CfirExtensionsResolveProcessor.beforePhase` 中用 `CfirProviderImpl.getAllFiles()`
 * 刷新 extend 索引；LL session 没有全量文件 provider，只能以当前 session 已构建的
 * `ModuleFileCache` 作为 lazy resolve 可见文件集合。这里在 provider 查询入口刷新同一份
 * [CfirExtendIndexStore]，然后复用主干 [CfirSessionExtendProvider] 的语义查询。
 *
 * 与 Kotlin 的偏离（显式记录）：Kotlin 镜像里**没有** extend provider
 * （`LLFirSessionExtendProvider` / `LLFirExtendableSymbolProvider` 均不存在），它靠
 * `LLFirSessionInvalidationService` + PSI/ProjectRoot modification tracker 让整个 session 失效。
 * 仓颉的 extend 是按「被扩展目标类型」建索引的派生数据，必须有一个按 session 重建的入口；
 * 这里以「已建索引的文件集」作为版本号，配合 [CfirExtendIndexStore.modificationCount]，
 * 等价于 Kotlin 的「session 快照内派生数据只建一次」。
 */
internal class LLCfirSessionExtendProvider(
    /**
     * 当前源码可解析模块 session。
     */
    private val session: LLCfirResolvableModuleSession,

    /**
     * 保存 extend 索引的 session 级存储。
     */
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendProvider {
    /** 版本读取只观察真实完成索引，首次目标查询或全量重建之后与主干发布同步。 */
    override val modificationCount: Long
        get() = indexStore.modificationCount

    /**
     * 复用主干 extend provider 的查询语义。
     */
    private val delegate = CfirSessionExtendProvider(session, indexStore)
    private val scopeSession = ScopeSession()

    /** 当前目标查询快照所使用的文件集合。 */
    private var indexedFiles: Set<CfirFile> = emptySet()

    /** 已写入结构 owner 索引的 CFIR 文件集合。 */
    @Volatile
    private var structurallyIndexedFiles: Set<CfirFile> = emptySet()

    /** 已按目标和声明包完成准备的定向查询。 */
    private val resolvedTargetQueries = hashSetOf<Pair<CfirExtendTargetKey, FqName?>>()

    /** 已上抛的框架缺陷在同一版本、同一查询中不重复执行。null 代表全量查询。 */
    private val failedIndexQueries = hashSetOf<Pair<CfirExtendTargetKey, FqName?>?>()

    /** 全部 extend 都已完成 TYPES 查询和索引重建。 */
    private var allExtendsResolved = false

    /** 防止类型头解析查询 extend provider 时重入当前构建。 */
    private var indexBuildInProgress = false

    private var lastBuildModificationCount = 0L

    /** 头部符号不可恢复的声明保留结构归属，到下一文件或 PSI 版本再尝试。 */
    private val degradedExtends = hashSetOf<CfirExtend>()

    /** 全量扩展枚举与定向查询使用相同的懒解析索引版本。 */
    override fun getAllExtends(): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getAllExtends()
    }

    /**
     * 查询目标 key 对应的扩展声明。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> {
        ensureIndexIsFresh(targetKey)
        return delegate.getExtendsForTarget(targetKey)
    }

    /** CJMP 查找同包候选时只准备同包、同目标的 extend。 */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey, packageFqName: FqName): List<CfirExtend> {
        ensureIndexIsFresh(targetKey, packageFqName)
        return delegate.getExtendsForTarget(targetKey, packageFqName)
    }

    /** CJMP 类型头比较只推进导入绑定；只把结构等价的 extend 交给后续 TYPES 阶段。 */
    override fun getCjmpMatchingExtends(target: CfirExtend, packageFqName: FqName): List<CfirExtend> {
        val phasesAtEntry = session.moduleComponents.cache.getAllCachedCfirFilesForResolution()
            .flatMap { it.declarations.filterIsInstance<CfirExtend>() }
            .associateWith { it.resolvePhase }
        fun checkUnrelatedExtendsRemainBeforeTypes(stage: String) {
            val advanced = phasesAtEntry.entries.firstOrNull { (extend, phase) ->
                phase < CfirResolvePhase.TYPES && extend.resolvePhase >= CfirResolvePhase.TYPES
            } ?: return
            val (extend, phase) = advanced
            check(false) {
                "CJMP candidate lookup advanced an existing extend to TYPES during $stage: " +
                        "extend=${extend.symbol}, before=$phase, after=${extend.resolvePhase}"
            }
        }
        val targetKey = target.extendedTypeRef.coneTypeOrNull?.expandedExtendTargetKey ?: return emptyList()
        val originalKey = target.cjmpOriginalExtendKey() ?: return emptyList()
        val comparisonSession = target.moduleData.session
        session.symbolProvider.materializeTopLevelExtendFiles()
        val files = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
        checkUnrelatedExtendsRemainBeforeTypes("source file materialization")
        val result = ArrayList<CfirExtend>()
        for (file in files) {
            if (file.moduleData != session.moduleData) continue
            if (file.packageDirective.packageFqName != packageFqName) continue
            file.lazyResolveToPhase(CfirResolvePhase.IMPORTS)
            checkUnrelatedExtendsRemainBeforeTypes("imports for ${file.name}")
            for (candidate in file.declarations.filterIsInstance<CfirExtend>()) {
                val phaseBeforeTargetInspection = candidate.resolvePhase
                val matchesTarget = candidate.resolvesExtendedTargetTo(targetKey, file)
                checkUnrelatedExtendsRemainBeforeTypes("target inspection for ${candidate.extendedTypeRef}")
                if (!matchesTarget) continue
                val candidateKey = candidate.resolveCjmpOriginalExtendKey(file)
                checkUnrelatedExtendsRemainBeforeTypes("full type-head inspection for ${candidate.extendedTypeRef}")
                if (candidateKey != null && originalKey.sameKeyAs(candidateKey, comparisonSession)) result += candidate
                check(candidate.resolvePhase == phaseBeforeTargetInspection) {
                    "CJMP type-head inspection advanced its candidate declaration: " +
                            "candidate=${candidate.symbol}, before=$phaseBeforeTargetInspection, " +
                            "after=${candidate.resolvePhase}"
                }
            }
        }
        checkUnrelatedExtendsRemainBeforeTypes("completed candidate lookup")
        return result
    }

    /**
     * 查询作用于指定 class id 的扩展声明。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> {
        ensureIndexIsFresh(CfirExtendTargetKey.ClassLike(classId))
        return delegate.getExtendsForClass(classId)
    }

    /**
     * 查询指定包中的扩展声明。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsInPackage(packageFqName)
    }

    /**
     * 查询作用于内建基本类型的扩展声明。
     */
    override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> {
        ensureIndexIsFresh()
        return delegate.getExtendsForBuiltinType(kind)
    }

    /**
     * 查询 [symbol] 所属的扩展声明。
     */
    override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? {
        ensureDeclarationSourcesAreFresh()
        return delegate.getContainingExtend(symbol)
    }

    /**
     * 查询 [extend] 所在包名。
     */
    override fun getPackageFqName(extend: CfirExtend): FqName? {
        indexStore.packageFqNameOf(extend)?.let { return it }
        ensureDeclarationSourcesAreFresh()
        return delegate.getPackageFqName(extend)
    }

    /** 查询 [extend] 的结构性源码文件归属。 */
    override fun getContainingFile(extend: CfirExtend): CfirFile? {
        indexStore.containingFileOf(extend)?.let { return it }
        ensureDeclarationSourcesAreFresh()
        return delegate.getContainingFile(extend)
    }

    /** 更新声明级 owner 索引；该查询只读取原始 CFIR 结构，不触发类型或继承语义解析。 */
    private fun ensureDeclarationSourcesAreFresh() {
        session.symbolProvider.materializeTopLevelExtendFiles()
        val files = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
        if (files.isEmpty()) return
        if (files.toSet() == structurallyIndexedFiles) return

        synchronized(this) {
            val latestFiles = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
            if (latestFiles.isEmpty()) return
            val latestFileSet = latestFiles.toSet()
            if (latestFileSet == structurallyIndexedFiles) return
            indexStore.recordDeclarationSources(latestFiles)
            structurallyIndexedFiles = latestFileSet
        }
    }

    /**
     * 定向查询只推进匹配目标与声明包的 extend；全量查询补齐索引。
     * 同一文件快照内保留结构信息，并在 PSI 修改后重试降级的声明。
     */
    private fun ensureIndexIsFresh(
        targetKey: CfirExtendTargetKey? = null,
        packageFqName: FqName? = null,
    ) {
        require(targetKey != null || packageFqName == null)
        session.symbolProvider.materializeTopLevelExtendFiles()
        val targetQuery = targetKey?.let { it to packageFqName }

        synchronized(this) {
            if (indexBuildInProgress) return

            val files = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
            if (files.isEmpty()) return
            val fileSet = files.toSet()
            val modificationCount = PsiModificationTracker.getInstance(session.project).modificationCount
            val retryDegraded = degradedExtends.isNotEmpty() && modificationCount != lastBuildModificationCount
            if (fileSet != indexedFiles || retryDegraded) {
                if (fileSet != structurallyIndexedFiles) {
                    indexStore.recordDeclarationSources(files)
                    structurallyIndexedFiles = fileSet
                }
                indexedFiles = fileSet
                resolvedTargetQueries.clear()
                failedIndexQueries.clear()
                allExtendsResolved = false
                degradedExtends.clear()
            }
            if (allExtendsResolved || targetQuery in failedIndexQueries ||
                targetQuery?.let(resolvedTargetQueries::contains) == true
            ) return

            indexBuildInProgress = true
            lastBuildModificationCount = modificationCount
            try {
                val initialExtendPhases = if (targetQuery == null) {
                    emptyMap()
                } else {
                    files.flatMap { it.declarations.filterIsInstance<CfirExtend>() }
                        .associateWith { it.resolvePhase }
                }
                val resolvedTargetExtends = hashSetOf<CfirExtend>()
                fun checkUnrelatedExtendsRemainBeforeTypes(stage: String) {
                    val advanced = initialExtendPhases.entries.firstOrNull { (extend, phase) ->
                        extend !in resolvedTargetExtends && phase < CfirResolvePhase.TYPES &&
                                extend.resolvePhase >= CfirResolvePhase.TYPES
                    } ?: return
                    val (extend, phase) = advanced
                    error(
                        "Targeted extend indexing advanced a different extend to TYPES during $stage: " +
                                "requested=${targetQuery?.first}, extend=${extend.symbol}, " +
                                "before=$phase, after=${extend.resolvePhase}"
                    )
                }

                for (file in files) {
                    if (packageFqName != null && file.packageDirective.packageFqName != packageFqName) continue
                    for (extend in file.declarations.filterIsInstance<CfirExtend>()) {
                        if (targetKey != null) {
                            val initialPhase = extend.resolvePhase
                            val matchesTarget = tryResolveExtendHead(extend) {
                                extend.resolvesExtendedTargetTo(targetKey, file)
                            } ?: continue
                            check(matchesTarget || initialPhase >= CfirResolvePhase.TYPES ||
                                    extend.resolvePhase < CfirResolvePhase.TYPES) {
                                "Inspecting a non-matching extend target advanced it to TYPES: " +
                                        "target=$targetKey, extend=${extend.symbol}, " +
                                        "before=$initialPhase, after=${extend.resolvePhase}"
                            }
                            if (!matchesTarget) continue
                            resolvedTargetExtends += extend
                        }
                        tryResolveExtendHead(extend) { extend.lazyResolveToPhase(CfirResolvePhase.TYPES) }
                    }
                }

                checkUnrelatedExtendsRemainBeforeTypes("head resolution")
                // 未完成 TYPES 的声明也保留文件和包归属；它们没有可索引的目标键。
                indexStore.rebuild(files, session.typeResolver)
                checkUnrelatedExtendsRemainBeforeTypes("index rebuild")
                if (indexStore.lastRebuildFailureCount > 0) {
                    LOG.warn(
                        "extend 语义建模失败 ${indexStore.lastRebuildFailureCount} 条，已按「目标索引缺席」降级",
                        indexStore.lastRebuildFailure,
                    )
                }

                if (targetQuery == null) {
                    allExtendsResolved = true
                } else {
                    resolvedTargetQueries += targetQuery
                }
            } catch (cancelled: ProcessCanceledException) {
                throw cancelled
            } catch (indexNotReady: IndexNotReadyException) {
                throw indexNotReady
            } catch (failure: Exception) {
                failedIndexQueries += targetQuery
                throw failure
            } finally {
                indexBuildInProgress = false
            }
        }
    }

    /** 单条 extend 的符号不可恢复不阻塞同文件的其它声明。 */
    private inline fun <T : Any> tryResolveExtendHead(extend: CfirExtend, resolve: () -> T): T? {
        if (extend in degradedExtends) return null
        return try {
            resolve()
        } catch (failure: CaSymbolUnavailableException) {
            LOG.warn("extend 头部解析失败，该条 extend 本轮缺席目标索引：$extend", failure)
            degradedExtends += extend
            null
        }
    }

    /** 解析类型头以验证目标身份，不推进候选 extend 自身的 resolve phase。 */
    private fun CfirExtend.resolvesExtendedTargetTo(
        requestedTargetKey: CfirExtendTargetKey,
        file: CfirFile,
    ): Boolean {
        file.lazyResolveToPhase(CfirResolvePhase.IMPORTS)
        val sourceTypeRef = (extendedTypeRef as? CfirResolvedTypeRef)?.delegatedTypeRef ?: extendedTypeRef
        val configuration = TypeResolutionConfiguration.EMPTY
            .withUseSiteFile(file)
            .withTopContainer(this)
            .withScopes(session.createFileLookupScopes(file, scopeSession).typeResolutionScopes)
            .withAdditionalTypeParameters(typeParameters)
        fun resolveTargetType(): ConeCangJieType = session.typeResolver.resolveType(
            typeRef = sourceTypeRef,
            configuration = configuration,
            areBareTypesAllowed = false,
            isOperandOfIsOperator = false,
            resolveDeprecations = true,
            supertypeSupplier = SupertypeSupplier.Default,
        ).type

        var targetType = extendedTypeRef.coneTypeOrNull ?: resolveTargetType()
        val visitedAliases = hashSetOf<ClassId>()
        while (true) {
            targetType.expandedExtendTargetKey?.let { return it == requestedTargetKey }
            val aliasType = targetType.unresolvedAliasOrNull() ?: return false
            if (!visitedAliases.add(aliasType.classId)) return false
            val alias = session.typeResolver.resolveClass(aliasType.classId) as? CfirTypeAlias ?: return false
            alias.lazyResolveToPhase(CfirResolvePhase.TYPES)
            targetType = resolveTargetType()
        }
    }

    /** 在候选原声明上计算完整 CJMP 类型头，不发布 resolved refs，也不推进候选声明 phase。 */
    private fun CfirExtend.resolveCjmpOriginalExtendKey(file: CfirFile): CfirCjmpExtendKey? = synchronized(scopeSession) {
        val configuration = TypeResolutionConfiguration.EMPTY
            .withUseSiteFile(file)
            .withTopContainer(this)
            .withScopes(session.createFileLookupScopes(file, scopeSession).typeResolutionScopes)
            .withAdditionalTypeParameters(typeParameters)

        fun resolveTypeRef(typeRef: CfirTypeRef): ConeCangJieType? {
            val sourceTypeRef = (typeRef as? CfirResolvedTypeRef)?.delegatedTypeRef ?: typeRef
            return session.typeResolver.resolveType(
                typeRef = sourceTypeRef,
                configuration = configuration,
                areBareTypesAllowed = false,
                isOperandOfIsOperator = false,
                resolveDeprecations = true,
                supertypeSupplier = SupertypeSupplier.Default,
            ).type.takeUnless { it.containsErrorType() }
        }

        val target = resolveTypeRef(extendedTypeRef) ?: return@synchronized null
        val interfaces = superTypeRefs.map { resolveTypeRef(it) ?: return@synchronized null }
        val bounds = typeParameters.map { parameter ->
            parameter.bounds.map { resolveTypeRef(it) ?: return@synchronized null }
        }
        CfirCjmpExtendKey(target, typeParameters.map { it.symbol }, interfaces, bounds)
    }

    private fun ConeCangJieType.unresolvedAliasOrNull(): ConeTypeAliasType? = when (this) {
        is ConeTypeAliasType -> {
            val expanded = expandedType
            if (expanded == null) this else expanded.unresolvedAliasOrNull()
        }
        is ConeErrorType -> delegatedType?.unresolvedAliasOrNull()
        else -> null
    }
}

/**
 * extend 索引重建的日志对象。
 */
private val LOG = Logger.getInstance("org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirSessionExtendProvider")
