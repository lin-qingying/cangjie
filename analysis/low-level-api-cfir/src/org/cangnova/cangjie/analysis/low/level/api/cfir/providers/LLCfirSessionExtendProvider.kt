package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

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

    @Volatile
    /**
     * 已经写入 [indexStore] 的 CFIR 文件集合。
     */
    private var indexedFiles: Set<CfirFile> = emptySet()

    /** 已写入结构 owner 索引的 CFIR 文件集合。 */
    @Volatile
    private var structurallyIndexedFiles: Set<CfirFile> = emptySet()

    /** 已按目标和声明包完成准备的定向查询。 */
    private val resolvedTargetQueries = hashSetOf<Pair<CfirExtendTargetKey, FqName?>>()

    /** 完整 extend 索引是否已由全部声明的 TYPES 阶段构建。 */
    private var allExtendsResolved: Boolean = false

    /** 当前线程是否正为这批文件建立 extend 索引，防止类型解析重入启动相同工作。 */
    private var indexBuildInProgress: Boolean = false

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
     * 确保当前 session 已缓存的顶层 extend 文件全部进入 [indexStore]。
     *
     * 方法先物化顶层 extend 文件，再比较缓存文件集合；集合发生变化时在同步块内解析 extend 头部类型并重建索引。
     */
    private fun ensureIndexIsFresh(
        targetKey: CfirExtendTargetKey? = null,
        packageFqName: FqName? = null,
    ) {
        require(targetKey != null || packageFqName == null)
        session.symbolProvider.materializeTopLevelExtendFiles()
        val files = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
        if (files.isEmpty()) return

        val targetQuery = targetKey?.let { it to packageFqName }

        synchronized(this) {
            if (indexBuildInProgress) return

            val latestFiles = session.moduleComponents.cache.getAllCachedCfirFilesForResolution().toList()
            val latestFileSet = latestFiles.toSet()
            if (latestFiles.isEmpty()) return
            if (latestFileSet != indexedFiles) {
                if (latestFileSet != structurallyIndexedFiles) {
                    indexStore.recordDeclarationSources(latestFiles)
                    structurallyIndexedFiles = latestFileSet
                }
                indexedFiles = latestFileSet
                resolvedTargetQueries.clear()
                allExtendsResolved = false
            }
            if (allExtendsResolved || targetQuery?.let(resolvedTargetQueries::contains) == true) return

            indexBuildInProgress = true
            try {
                val initialExtendPhases = if (targetQuery == null) {
                    emptyMap()
                } else {
                    latestFiles.flatMap { it.declarations.filterIsInstance<CfirExtend>() }
                        .associateWith { it.resolvePhase }
                }
                val resolvedTargetExtends = hashSetOf<CfirExtend>()
                if (targetQuery == null) {
                    latestFiles.forEach(::resolveTopLevelExtendsToTypes)
                } else {
                    val (requestedTargetKey, requestedPackage) = targetQuery
                    for (file in latestFiles) {
                        if (requestedPackage != null && file.packageDirective.packageFqName != requestedPackage) continue
                        for (extend in file.declarations.filterIsInstance<CfirExtend>()) {
                            val initialPhase = extend.resolvePhase
                            val matchesTarget = extend.resolvesExtendedTargetTo(requestedTargetKey, file)
                            check(matchesTarget || initialPhase >= CfirResolvePhase.TYPES ||
                                    extend.resolvePhase < CfirResolvePhase.TYPES) {
                                "Inspecting a non-matching extend target advanced it to TYPES: " +
                                        "target=$requestedTargetKey, extend=${extend.symbol}, " +
                                        "before=$initialPhase, after=${extend.resolvePhase}"
                            }
                            if (matchesTarget) {
                                extend.lazyResolveToPhase(CfirResolvePhase.TYPES)
                                resolvedTargetExtends += extend
                            }
                        }
                    }
                    check(initialExtendPhases.all { (extend, phase) ->
                        extend in resolvedTargetExtends || phase >= CfirResolvePhase.TYPES ||
                                extend.resolvePhase < CfirResolvePhase.TYPES
                    }) {
                        initialExtendPhases.entries.first { (extend, phase) ->
                            extend !in resolvedTargetExtends && phase < CfirResolvePhase.TYPES &&
                                    extend.resolvePhase >= CfirResolvePhase.TYPES
                        }.let { (extend, phase) ->
                            "Targeted TYPES resolution advanced a different extend to TYPES before index rebuild: " +
                                    "requested=$requestedTargetKey, extend=${extend.symbol}, " +
                                    "before=$phase, after=${extend.resolvePhase}"
                        }
                    }
                }
                indexStore.rebuild(latestFiles, session.typeResolver)
                if (targetQuery != null) {
                    check(initialExtendPhases.all { (extend, phase) ->
                        extend in resolvedTargetExtends || phase >= CfirResolvePhase.TYPES ||
                                extend.resolvePhase < CfirResolvePhase.TYPES
                    }) {
                        initialExtendPhases.entries.first { (extend, phase) ->
                            extend !in resolvedTargetExtends && phase < CfirResolvePhase.TYPES &&
                                    extend.resolvePhase >= CfirResolvePhase.TYPES
                        }.let { (extend, phase) ->
                            "Building the targeted extend index advanced a different extend to TYPES: " +
                                    "requested=${targetQuery.first}, extend=${extend.symbol}, " +
                                    "before=$phase, after=${extend.resolvePhase}"
                        }
                    }
                }
                // 只有索引重建和阶段不变量检查都成功，才能发布查询完成状态。
                if (targetQuery == null) {
                    allExtendsResolved = true
                } else {
                    resolvedTargetQueries += targetQuery
                }
            } finally {
                indexBuildInProgress = false
            }
        }
    }

/**
     * 将 [file] 中的顶层 extend 声明推进到 [CfirResolvePhase.TYPES]。
     */
    private fun resolveTopLevelExtendsToTypes(file: CfirFile) {
        for (declaration in file.declarations) {
            if (declaration is CfirExtend) {
                declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
            }
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
