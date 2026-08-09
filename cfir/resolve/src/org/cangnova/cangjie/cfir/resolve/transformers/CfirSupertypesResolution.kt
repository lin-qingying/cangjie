package org.cangnova.cangjie.cfir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirEnumImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirExtendImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirStructImpl
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind

import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.DeclaredSupertypeClassification
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.inheritanceCycleDependencyTypeOrNull
import org.cangnova.cangjie.cfir.resolve.providers.ordinarySupertypeTypeOrNull
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.superTypeGraphStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId

import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRefCopy
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.types.forEachType
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.util.PrivateForInline

/**
 * SUPER_TYPES 阶段的主处理器。
 *
 * 该处理器在正式转换文件前先收集 extend 声明形成的额外超类型边，
 * 再由 [CfirSupertypeResolverTransformer] 解析、断环并回写 class-like 超类型。
 */
internal class CfirSupertypeResolverProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.SUPER_TYPES,
) {
    /** SUPER_TYPES 阶段实际使用的树转换器。 */
    override val transformer: CfirSupertypeResolverTransformer =
        CfirSupertypeResolverTransformer(session, scopeSession)

    /**
     * 阶段开始前预采集 extend 超类型图。
     *
     * extend 边会参与继承环归因，因此必须在普通 class-like 超类型断环之前写入图存储。
     */
    override fun beforePhase() {
        super.beforePhase()

        val files = (runCatching { session.cfirProvider }.getOrNull() as? CfirProviderImpl)?.getAllFiles().orEmpty()
        if (files.isEmpty()) return

        session.superTypeGraphStoreOrNull?.let { graphStore ->
            graphStore.clearExtended()
            CfirExtendSupertypesCollector(session, scopeSession, graphStore).collect(files)
        }
    }
}

/**
 * SUPER_TYPES 阶段的文件级转换器。
 *
 * 它先通过 visitor 收集并解析所有 class-like 超类型，再执行继承环修正，
 * 最后用 apply transformer 把解析结果写回原 CFIR 树。
 */
internal class CfirSupertypeResolverTransformer(
    /** 当前 SUPER_TYPES 阶段使用的会话。 */
    override val session: CfirSession,
    scopeSession: ScopeSession,
) : CfirAbstractPhaseTransformer<Any?>(CfirResolvePhase.SUPER_TYPES) {
    /** 本文件解析过程共享的超类型计算缓存。 */
    private val supertypeComputationSession = SupertypeComputationSession()
    /** 负责遍历声明并计算超类型的 visitor。 */
    private val supertypeResolverVisitor = CfirSupertypeResolverVisitor(session, supertypeComputationSession, scopeSession)
    /** 负责把计算结果写回声明树的 transformer。 */
    private val applySupertypesTransformer = CfirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)

    /** SUPER_TYPES 阶段不处理通用元素，未知元素保持原样。 */
    override fun <E : CfirElement> transformElement(element: E, data: Any?): E = element

    /** 解析单个文件中的所有 class-like 超类型并回写 SUPER_TYPES phase。 */
    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            file.accept(supertypeResolverVisitor, null)
            supertypeComputationSession.breakLoops(session, supertypeResolverVisitor.localClassesNavigationInfo)
            file.transform(applySupertypesTransformer, null)
        }
    }
}

/**
 * 对局部 class-like 声明单独执行 SUPER_TYPES 阶段。
 *
 * low-level body resolve 会在局部声明被需求触发时调用该入口，复用当前文件 scope、
 * 局部类导航信息和容器链来保证类型解析上下文与完整文件解析一致。
 */
fun <F : CfirClassLikeDeclaration> F.runSupertypeResolvePhaseForLocalClass(
    session: CfirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<CfirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
    useSiteFile: CfirFile,
    containingDeclarations: List<CfirDeclaration>,
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = CfirSupertypeResolverVisitor(
        session = session,
        supertypeComputationSession = supertypeComputationSession,
        scopeSession = scopeSession,
        scopeForLocalClass = currentScopeList.toPersistentList(),
        localClassesNavigationInfo = localClassesNavigationInfo,
        useSiteFile = useSiteFile,
        containingDeclarations = containingDeclarations,
    )
    accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session, localClassesNavigationInfo)
    val applySupertypesTransformer = CfirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)
    @Suppress("UNCHECKED_CAST")
    return this.transform<CfirClassLikeDeclaration, Any?>(applySupertypesTransformer, null) as F
}

/**
 * low-level API 对非局部声明执行 SUPER_TYPES 阶段的主干入口。
 *
 * 它复用编译器主干的 visitor / loop breaking / apply transformer，
 * 只是去掉 local-class 专用上下文，供 IDE 的 designated lazy resolve 直接调用。
 */
fun <F : CfirClassLikeDeclaration> F.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration(
    session: CfirSession,
    scopeSession: ScopeSession,
    useSiteFile: CfirFile?,
    containingDeclarations: List<CfirDeclaration> = emptyList(),
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = CfirSupertypeResolverVisitor(
        session = session,
        supertypeComputationSession = supertypeComputationSession,
        scopeSession = scopeSession,
        useSiteFile = useSiteFile,
        containingDeclarations = containingDeclarations,
    )
    accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session, localClassesNavigationInfo = null)
    val applySupertypesTransformer = CfirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)
    @Suppress("UNCHECKED_CAST")
    return this.transform<CfirClassLikeDeclaration, Any?>(applySupertypesTransformer, null) as F
}

/**
 * 单个 class-like 声明超类型计算的生命周期状态。
 */
sealed class SupertypeComputationStatus {
    /** 尚未开始解析该声明的超类型。 */
    data object NotComputed : SupertypeComputationStatus()
    /** 该声明的超类型正在解析中，用于发现递归继承。 */
    data object Computing : SupertypeComputationStatus()
    /** 该声明的超类型已经解析完成。 */
    class Computed(val supertypeRefs: List<CfirResolvedTypeRef>) : SupertypeComputationStatus()
}

/**
 * SUPER_TYPES 阶段的共享计算会话。
 *
 * 它缓存文件导入 scope、typealias 展开类型、class-like 计算状态和 classId 到声明的索引，
 * 供超类型解析、typealias 展开和继承环修正共同使用。
 */
open class SupertypeComputationSession {
    /** 文件到导入 scope 列表的缓存，避免同一文件重复构造 scope。 */
    private val fileScopes: MutableMap<CfirFile, ScopePersistentList> = linkedMapOf()
    /** typealias 到已解析 expanded typeRef 的缓存。 */
    private val resolvedExpandedTypeRefs: MutableMap<CfirTypeAlias, CfirResolvedTypeRef> = linkedMapOf()
    /** class-like 声明到超类型计算状态的缓存。 */
    private val computationStatus: MutableMap<CfirClassLikeDeclaration, SupertypeComputationStatus> =
        linkedMapOf<CfirClassLikeDeclaration, SupertypeComputationStatus>().withDefault { SupertypeComputationStatus.NotComputed }
    /** 当前阶段已见 classId 到声明节点的索引。 */
    private val declarationIndex: MutableMap<ClassId, CfirClassLikeDeclaration> = linkedMapOf()

    /**
     * 类型解析器查询已解析超类型时使用的 supplier。
     *
     * 该 supplier 只返回当前计算会话已知声明的 cone type，未见声明返回空列表。
     */
    val supertypesSupplier: SupertypeSupplier = SupertypeSupplier { classId ->
        val declaration = declarationIndex[classId]
        supertypeRefs(declaration).mapNotNull { typeRef ->
            typeRef.classifyDeclaredSupertype(expandType = { it }).ordinarySupertypeTypeOrNull()
        }
    }

    /** 记录当前阶段已经访问到的 class-like 声明。 */
    fun rememberDeclaration(classLikeDeclaration: CfirClassLikeDeclaration) {
        declarationIndex[classLikeDeclaration.symbol.classId] = classLikeDeclaration
    }

    /** 获取文件 scope 缓存；不存在时使用 [builder] 构造并保存。 */
    fun getOrPutFileScope(file: CfirFile, builder: () -> ScopePersistentList): ScopePersistentList =
        fileScopes.getOrPut(file, builder)

    /** 标记指定 class-like 声明开始计算超类型。 */
    fun startComputingSupertypes(classLikeDeclaration: CfirClassLikeDeclaration) {
        require(getSupertypesComputationStatus(classLikeDeclaration) is SupertypeComputationStatus.NotComputed) {
            "Unexpected supertype computation status for ${classLikeDeclaration.symbol.classId.asString()}: " +
                getSupertypesComputationStatus(classLikeDeclaration)
        }
        computationStatus[classLikeDeclaration] = SupertypeComputationStatus.Computing
    }

    /** 保存指定 class-like 声明解析完成的直接超类型列表。 */
    fun storeSupertypes(classLikeDeclaration: CfirClassLikeDeclaration, supertypeRefs: List<CfirResolvedTypeRef>) {
        require(getSupertypesComputationStatus(classLikeDeclaration) is SupertypeComputationStatus.Computing) {
            "Unexpected supertype computation status for ${classLikeDeclaration.symbol.classId.asString()}: " +
                getSupertypesComputationStatus(classLikeDeclaration)
        }
        computationStatus[classLikeDeclaration] = SupertypeComputationStatus.Computed(supertypeRefs)
        if (classLikeDeclaration is CfirTypeAlias && supertypeRefs.size == 1) {
            resolvedExpandedTypeRefs[classLikeDeclaration] = supertypeRefs.single()
        }
    }

    /** 返回指定 class-like 声明当前的超类型计算状态。 */
    fun getSupertypesComputationStatus(classLikeDeclaration: CfirClassLikeDeclaration): SupertypeComputationStatus =
        computationStatus.getValue(classLikeDeclaration)

    /**
     * 获取已解析的直接超类型列表。
     *
     * 如果声明尚未在本会话中完成计算，则退回到声明上已有的 resolved typeRef。
     */
    fun getResolvedSupertypeRefs(classLikeDeclaration: CfirClassLikeDeclaration): List<CfirResolvedTypeRef> {
        val status = computationStatus[classLikeDeclaration]
        if (status is SupertypeComputationStatus.Computed) return status.supertypeRefs
        if (classLikeDeclaration is CfirTypeAlias) {
            resolvedExpandedTypeRefs[classLikeDeclaration]?.let { return listOf(it) }
        }
        return classLikeDeclaration.superTypeRefs.filterIsInstance<CfirResolvedTypeRef>()
    }

    /** 获取 typealias 已解析 expanded typeRef；缺失时构造错误类型占位。 */
    fun getResolvedExpandedTypeRef(typeAlias: CfirTypeAlias): CfirResolvedTypeRef =
        resolvedExpandedTypeRefs[typeAlias]
            ?: getResolvedSupertypeRefs(typeAlias).singleOrNull()
            ?: createErrorTypeRef(typeAlias.source, "Expanded type for ${typeAlias.symbol.classId.asString()} was not computed")

    /**
     * 返回指定 class-like 声明的当前超类型引用。
     *
     * 已计算声明返回计算结果，typealias 优先返回 expanded typeRef，否则返回声明原始 superTypeRefs。
     */
    fun supertypeRefs(classLikeDeclaration: CfirClassLikeDeclaration?): List<CfirTypeRef> {
        if (classLikeDeclaration == null) return emptyList()
        val status = computationStatus[classLikeDeclaration]
        if (status is SupertypeComputationStatus.Computed) return status.supertypeRefs
        if (classLikeDeclaration is CfirTypeAlias) {
            resolvedExpandedTypeRefs[classLikeDeclaration]?.let { return listOf(it) }
        }
        return classLikeDeclaration.superTypeRefs
    }

    /**
     * 对齐 Kotlin `SupertypeComputationSession.expandTypealiasInPlace(...)`：
     * `SUPER_TYPES` 阶段是否真的把 alias 改写成展开类型，必须受 session 里的全局 analysis flag 控制，
     * 不能在这里无条件把 `ConeTypeAliasType` 抹平成真实类型。
     */
    fun expandTypealiasInPlace(typeRef: CfirResolvedTypeRef, session: CfirSession): CfirResolvedTypeRef {
        if (!session.languageVersionSettings.getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)) {
            return typeRef
        }

        val expandedType = typeRef.coneType.fullyExpandedType(session, ::getResolvedExpandedType)
        if (expandedType == typeRef.coneType) return typeRef
        val expandedTypeRef = buildResolvedTypeRefCopy(typeRef) {
            coneType = expandedType
        }
        return if (expandedTypeRef.coneType is ConeErrorType) {
            expandedTypeRef.toErrorTypeRef()
        } else {
            expandedTypeRef
        }
    }

    /** 返回 typealias 已解析 expanded type 的 cone type 表示。 */
    fun getResolvedExpandedType(typeAlias: CfirTypeAlias): ConeCangJieType? =
        getResolvedExpandedTypeRef(typeAlias).coneTypeSafe()

    /**
     * 对已解析超类型和 typealias 展开结果执行继承环修正。
     *
     * extend 参与的环优先归因到 extend 边，普通 class-like 环再回写到对应 typeRef。
     */
    fun breakLoops(session: CfirSession, localClassesNavigationInfo: LocalClassesNavigationInfo?) {
        val declarations = LinkedHashSet<CfirClassLikeDeclaration>()
        declarations += declarationIndex.values
        declarations += localClassesNavigationInfo?.parentForClass?.keys.orEmpty()
        breakTypeAliasLoops(declarations.filterIsInstance<CfirTypeAlias>(), session)
        val extendCoveredDeclarations = reportExtendInheritanceCycles(declarations, session)

        for (declaration in declarations) {
            if (declaration in extendCoveredDeclarations) continue
            when (declaration) {
                is CfirTypeAlias -> {
                    val original = resolvedExpandedTypeRefs[declaration] ?: continue
                    val rewritten = breakLoopsInTypeRef(declaration, original, session, listOf(declaration))
                    if (rewritten !== original) {
                        resolvedExpandedTypeRefs[declaration] = rewritten
                        computationStatus[declaration] = SupertypeComputationStatus.Computed(listOf(rewritten))
                    }
                }

                else -> {
                    val refs = getResolvedSupertypeRefs(declaration)
                    val rewritten = refs.map { breakLoopsInTypeRef(declaration, it, session, listOf(declaration)) }
                    if (rewritten != refs) {
                        computationStatus[declaration] = SupertypeComputationStatus.Computed(rewritten)
                    }
                }
            }
        }
    }

    /**
     * 在任何 typealias 展开前检查完整 RHS 类型图，并把环上每个声明改写为结构化错误。
     *
     * 官方 `TypeAliasCircleCheck` 会遍历 RHS 根类型和所有嵌套类型；因此这里按 alias 声明
     * 建图，而不是调用 [fullyExpandedType]。后续展开器只消费已经断环的图，不承担兜底职责。
     */
    private fun breakTypeAliasLoops(typeAliases: List<CfirTypeAlias>, session: CfirSession) {
        val visitStatus = linkedMapOf<CfirTypeAlias, InheritanceVisitStatus>()
        val path = ArrayList<CfirTypeAlias>()
        val cyclePathByAlias = linkedMapOf<CfirTypeAlias, String>()

        fun recordCycle(cycle: List<CfirTypeAlias>) {
            for ((index, typeAlias) in cycle.withIndex()) {
                cyclePathByAlias.putIfAbsent(
                    typeAlias,
                    (cycle.drop(index) + cycle.take(index) + typeAlias)
                        .joinToString("->") { declaration -> declaration.name.asString() },
                )
            }
        }

        fun visit(typeAlias: CfirTypeAlias) {
            when (visitStatus[typeAlias]) {
                InheritanceVisitStatus.Visiting -> {
                    val cycleStart = path.indexOf(typeAlias).takeIf { it >= 0 } ?: return
                    recordCycle(path.drop(cycleStart))
                    return
                }

                InheritanceVisitStatus.Visited -> return
                null -> Unit
            }

            visitStatus[typeAlias] = InheritanceVisitStatus.Visiting
            path += typeAlias
            for (dependency in directTypeAliasDependencies(typeAlias, session)) {
                visit(dependency)
            }
            path.removeAt(path.lastIndex)
            visitStatus[typeAlias] = InheritanceVisitStatus.Visited
        }

        typeAliases.forEach(::visit)
        for ((typeAlias, cyclePath) in cyclePathByAlias) {
            val originalTypeRef = resolvedExpandedTypeRefs[typeAlias] ?: continue
            val errorTypeRef = createErrorTypeRef(
                sourceElement = originalTypeRef.source ?: typeAlias.expandedTypeRef.source,
                message = cyclePath,
                kind = DiagnosticKind.RecursiveTypealiasExpansion,
                delegatedTypeRef = originalTypeRef,
            )
            resolvedExpandedTypeRefs[typeAlias] = errorTypeRef
            computationStatus[typeAlias] = SupertypeComputationStatus.Computed(listOf(errorTypeRef))
        }
    }

    /** 返回 typealias RHS 完整类型树中直接引用的所有 typealias 声明。 */
    private fun directTypeAliasDependencies(
        typeAlias: CfirTypeAlias,
        session: CfirSession,
    ): List<CfirTypeAlias> {
        val expandedType = resolvedExpandedTypeRefs[typeAlias]?.coneType ?: return emptyList()
        val dependencies = linkedSetOf<CfirTypeAlias>()
        expandedType.forEachType { nestedType ->
            val dependency = nestedType.toReferencedDeclaration(session) as? CfirTypeAlias ?: return@forEachType
            dependencies += dependency
        }
        return dependencies.toList()
    }

    /**
     * 检查单个已解析 typeRef 是否会形成继承环，并在需要时替换为错误 typeRef。
     *
     * [currentPath] 保存当前递归路径，用于区分直接自环、间接继承环和 typealias 递归展开。
     */
    private fun breakLoopsInTypeRef(
        owner: CfirClassLikeDeclaration,
        typeRef: CfirResolvedTypeRef,
        session: CfirSession,
        currentPath: List<CfirClassLikeDeclaration>,
    ): CfirResolvedTypeRef {
        val target = typeRef.inheritanceCycleTarget(session) ?: return typeRef
        val pathSet = currentPath.toSet()
        val hitsCurrentPath = target in pathSet || reachesAny(target, pathSet, session, mutableSetOf())
        if (!hitsCurrentPath) return typeRef

        val message = if (owner is CfirTypeAlias) {
            owner.name.asString()
        } else if (target == owner) {
            "Self-reference in supertype definition for ${owner.symbol.classId.asString()}"
        } else {
            "Loop in supertype definition for ${owner.symbol.classId.asString()}"
        }
        return createErrorTypeRef(
            sourceElement = if (target == owner || owner is CfirTypeAlias) typeRef.source else owner.source ?: typeRef.source,
            message = message,
            kind = when {
                owner is CfirTypeAlias -> DiagnosticKind.RecursiveTypealiasExpansion
                target == owner -> DiagnosticKind.SupertypeSelfReference
                else -> DiagnosticKind.LoopInSupertype
            },
            delegatedTypeRef = typeRef,
        )
    }

    /**
     * 官方 PreCheck 的继承环 DFS 会把从 extend 边进入的环归因到 extend 声明，
     * 并把环内声明标记为已访问，从而避免再在接口声明上报告第二个环诊断。
     */
    private fun reportExtendInheritanceCycles(
        declarations: Collection<CfirClassLikeDeclaration>,
        session: CfirSession,
    ): Set<CfirClassLikeDeclaration> {
        val graphStore = session.superTypeGraphStoreOrNull ?: return emptySet()
        val status = linkedMapOf<CfirClassLikeDeclaration, InheritanceVisitStatus>()
        val path = ArrayList<CfirClassLikeDeclaration>()
        val coveredByExtend = linkedSetOf<CfirClassLikeDeclaration>()
        val reportedExtendEdges = linkedSetOf<CfirSuperTypeGraphEdge>()

        fun dfs(
            declaration: CfirClassLikeDeclaration,
            activeExtendEdge: CfirSuperTypeGraphEdge?,
        ) {
            when (status[declaration]) {
                InheritanceVisitStatus.Visiting -> {
                    if (activeExtendEdge != null && reportedExtendEdges.add(activeExtendEdge)) {
                        activeExtendEdge.replaceWithInheritanceCycleError()
                        val cycleStart = path.indexOf(declaration).takeIf { it >= 0 } ?: 0
                        coveredByExtend += path.drop(cycleStart)
                    }
                    return
                }

                InheritanceVisitStatus.Visited -> return
                null -> Unit
            }

            status[declaration] = InheritanceVisitStatus.Visiting
            path += declaration
            for (dependency in directDependencies(declaration, session)) {
                dfs(dependency, activeExtendEdge)
            }
            for (edge in graphStore.getNode(declaration.symbol.classId)?.extendedSuperTypes.orEmpty()) {
                val target = edge.typeRef.toReferencedDeclaration(session) ?: continue
                dfs(target, edge)
            }
            path.removeAt(path.lastIndex)
            status[declaration] = InheritanceVisitStatus.Visited
        }

        for (declaration in declarations) {
            dfs(declaration, activeExtendEdge = null)
        }
        return coveredByExtend
    }

    /** 将 extend 图边替换成继承环错误类型引用。 */
    private fun CfirSuperTypeGraphEdge.replaceWithInheritanceCycleError() {
        val extend = sourceExtend ?: return
        val errorTypeRef = createErrorTypeRef(
            sourceElement = extend.source ?: typeRef.source,
            message = "Loop in supertypes involving $renderedType",
            kind = DiagnosticKind.LoopInSupertype,
            delegatedTypeRef = typeRef,
        )
        extend.replaceSuperTypeRef(typeRef, errorTypeRef)
    }

    /** 判断 [declaration] 是否能沿普通超类型依赖到达任一 [targets]。 */
    private fun reachesAny(
        declaration: CfirClassLikeDeclaration,
        targets: Set<CfirClassLikeDeclaration>,
        session: CfirSession,
        visited: MutableSet<CfirClassLikeDeclaration>,
    ): Boolean {
        if (!visited.add(declaration)) return false
        val directDependencies = directDependencies(declaration, session)
        for (dependency in directDependencies) {
            if (dependency in targets) return true
            if (reachesAny(dependency, targets, session, visited)) return true
        }
        return false
    }

    /** 返回声明在继承图中的普通直接依赖节点。 */
    private fun directDependencies(
        declaration: CfirClassLikeDeclaration,
        session: CfirSession,
    ): List<CfirClassLikeDeclaration> {
        val refs = when (declaration) {
            is CfirTypeAlias -> listOf(getResolvedExpandedTypeRef(declaration))
            else -> getResolvedSupertypeRefs(declaration)
        }
        return refs.mapNotNull { ref ->
            val target = ref.inheritanceCycleTarget(session) ?: return@mapNotNull null
            target.takeIf { declaration !is CfirInterface || it is CfirInterface }
        }
    }

    /**
     * 解析继承环 DFS 使用的声明目标。
     *
     * 该入口刻意独立于普通父类型与成员图：错误泛型实参数量仍可恢复 class owner 参与
     * 官方继承环检查，但不能因此成为类型关系或成员作用域中的有效父边。
     */
    private fun CfirResolvedTypeRef.inheritanceCycleTarget(
        session: CfirSession,
    ): CfirClassLikeDeclaration? {
        val classification = classifyDeclaredSupertype(
            session = session,
            expandType = { type -> type.fullyExpandedType(session, ::getResolvedExpandedType) },
        )
        val dependencyType = classification.inheritanceCycleDependencyTypeOrNull() ?: return null

        // 普通 typealias 父引用必须先保留 alias 声明节点，再由 DFS 展开其目标。
        // 若在这里直接把 alias 展开到 owner，本应是间接继承环的 `A <: Alias<A>`
        // 会被错误降成直接 `SUPER_TYPES_SELF_REFERENCE`。
        if (classification is DeclaredSupertypeClassification.ValidNominal) {
            coneType.toReferencedDeclaration(session)?.let { return it }
        }
        return dependencyType.toReferencedDeclaration(session)
    }
}

/** SUPER_TYPES 阶段内部使用的 scope 持久列表别名。 */
private typealias ScopePersistentList = PersistentList<CfirScope>

/**
 * 解析 class-like 声明超类型的 visitor。
 *
 * visitor 只负责计算并保存结果，不直接修改声明树；最终写回由 [CfirApplySupertypesTransformer] 统一完成。
 */
internal open class CfirSupertypeResolverVisitor(
    /** 当前超类型解析使用的会话。 */
    private val session: CfirSession,
    /** 当前阶段共享的超类型计算缓存。 */
    private val supertypeComputationSession: SupertypeComputationSession,
    /** 当前阶段共享的 scope 缓存会话。 */
    private val scopeSession: ScopeSession,
    /** 局部类解析时由调用方提供的当前 scope 列表。 */
    private val scopeForLocalClass: PersistentList<CfirScope>? = null,
    /** 局部类导航信息，用于后续继承环断开时补充局部声明集合。 */
    val localClassesNavigationInfo: LocalClassesNavigationInfo? = null,
    /** 当前解析的 use-site 文件。 */
    @property:PrivateForInline var useSiteFile: CfirFile? = null,
    containingDeclarations: List<CfirDeclaration> = emptyList(),
) : CfirDefaultVisitor<Unit, Any?>() {
    /** 当前 visitor 路径上的 class-like 容器栈。 */
    @PrivateForInline
    val classDeclarationsStack: ArrayDeque<CfirClassLikeDeclaration> = ArrayDeque()

    init {
        containingDeclarations.forEach {
            if (it is CfirClassLikeDeclaration) {
                @OptIn(PrivateForInline::class)
                classDeclarationsStack.addLast(it)
            }
        }
    }

    /**
     * 在指定文件上下文中执行 [block]。
     *
     * 该函数保证嵌套访问结束后恢复原 use-site 文件。
     */
    @OptIn(PrivateForInline::class)
    inline fun <R> withFile(file: CfirFile, block: () -> R): R {
        val oldFile = useSiteFile
        return try {
            useSiteFile = file
            block()
        } finally {
            useSiteFile = oldFile
        }
    }

    /** 默认 visitor 不处理非 class-like 元素。 */
    override fun visitElement(element: CfirElement, data: Any?) = Unit

    /** 在文件上下文中访问所有子声明。 */
    override fun visitFile(file: CfirFile, data: Any?) {
        withFile(file) {
            file.acceptChildren(this, data)
        }
    }

    /** 解析 class 的直接超类型并继续访问其声明内容。 */
    override fun visitClass(klass: CfirClass, data: Any?) {
        withClassLike(klass) {
            resolveSpecificClassLikeSupertypes(klass, klass.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(klass, data)
        }
    }

    /** 解析 interface 的直接超类型并继续访问其声明内容。 */
    override fun visitInterface(`interface`: CfirInterface, data: Any?) {
        withClassLike(`interface`) {
            resolveSpecificClassLikeSupertypes(`interface`, `interface`.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(`interface`, data)
        }
    }

    /** 解析 struct 的直接超类型并继续访问其声明内容。 */
    override fun visitStruct(struct: CfirStruct, data: Any?) {
        withClassLike(struct) {
            resolveSpecificClassLikeSupertypes(struct, struct.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(struct, data)
        }
    }

    /** 解析 enum 的直接超类型并继续访问其声明内容。 */
    override fun visitEnum(enum: CfirEnum, data: Any?) {
        withClassLike(enum) {
            resolveSpecificClassLikeSupertypes(enum, enum.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(enum, data)
        }
    }

    /** 解析 typealias 的 expanded type 并继续访问其声明内容。 */
    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: Any?) {
        withClassLike(typeAlias) {
            resolveTypeAliasSupertype(typeAlias)
            visitDeclarationContent(typeAlias, data)
        }
    }

    /** 访问声明内部的子声明。 */
    private fun visitDeclarationContent(declaration: CfirDeclaration, data: Any?) {
        declaration.acceptChildren(this, data)
    }

    /**
     * 在 class-like 容器栈中临时压入声明并执行 [body]。
     *
     * 栈内容用于类型解析配置中的 containing class declarations。
     */
    inline fun <T> withClassLike(classLikeDeclaration: CfirClassLikeDeclaration, body: () -> T): T {
        @OptIn(PrivateForInline::class)
        classDeclarationsStack.addLast(classLikeDeclaration)
        return try {
            body()
        } finally {
            @OptIn(PrivateForInline::class)
            classDeclarationsStack.removeLast()
        }
    }

    /** 准备文件级导入 scope，并复用计算会话中的文件 scope 缓存。 */
    private fun prepareFileScopes(file: CfirFile): ScopePersistentList {
        return supertypeComputationSession.getOrPutFileScope(file) {
            // 这里返回的顺序必须与 TypeResolutionConfiguration 的契约一致：
            // 高优先级 scope 在前，低优先级 scope 在后。
            // SUPER_TYPES 阶段如果把它反转，就会让默认导入先于当前文件声明命中，
            // 从而把本地 `Box/Hashable` 解析成 `std.core.*`。
            createImportingScopes(file, session).toPersistentList()
        }
    }

    /**
     * supertype 解析始终从 use-site 文件导入作用域起步，
     * 再附加当前声明自身的类型参数作用域。
     */
    private fun prepareScopes(classLikeDeclaration: CfirClassLikeDeclaration): ScopePersistentList {
        @OptIn(PrivateForInline::class)
        val fileScopes = useSiteFile?.let(::prepareFileScopes) ?: persistentListOf()
        return fileScopes.pushIfNotNull(classLikeDeclaration.typeParametersScope())
    }

    /**
     * 解析 typealias 的 expanded type。
     *
     * 递归 typealias 会被转换成错误 typeRef；正常结果写入 [SupertypeComputationSession]。
     */
    private fun resolveTypeAliasSupertype(typeAlias: CfirTypeAlias): CfirResolvedTypeRef {
        return resolveSpecificClassLikeSupertypes(typeAlias) { transformer, configuration ->
            val transformed = typeAlias.expandedTypeRef.transform<CfirTypeRef, CfirTypeResolutionConfiguration>(
                transformer,
                configuration,
            )
            val resolvedTypeRef = when (transformed) {
                is CfirResolvedTypeRef -> transformed
                else -> createErrorTypeRef(
                    typeAlias.expandedTypeRef.source,
                    "Unresolved expanded type: ${typeAlias.expandedTypeRef.renderReadable()}",
                    DiagnosticKind.UnresolvedSupertype,
                )
            }

            resolvedTypeRef.coneType.forEachType { nestedType ->
                val referencedTypeAlias = nestedType.toReferencedDeclaration(session) as? CfirTypeAlias
                    ?: return@forEachType
                visitTypeAlias(referencedTypeAlias, null)
            }
            listOf(resolvedTypeRef)
        }.single()
    }

    /**
     * 解析指定 class-like 声明的直接超类型列表。
     *
     * [resolveRecursively] 为 true 时会主动解析被引用的 typealias，保证后续断环能看到完整展开链。
     */
    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        supertypeRefs: List<CfirTypeRef>,
        resolveRecursively: Boolean,
    ): List<CfirResolvedTypeRef> {
        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer, configuration ->
            supertypeRefs.mapTo(mutableListOf()) { superTypeRef ->
                val transformed = superTypeRef.transform<CfirTypeRef, CfirTypeResolutionConfiguration>(transformer, configuration)
                val typeParameterType = transformed.coneTypeSafe<ConeTypeParameterType>()
                val referencedTypeAlias = transformed.coneTypeSafe<ConeTypeAliasType>()
                    ?.let { session.symbolProvider.getClassLikeSymbolByClassId(it.classId)?.cfir as? CfirTypeAlias }

                if (resolveRecursively && referencedTypeAlias != null) {
                    visitTypeAlias(referencedTypeAlias, null)
                }

                when {
                    typeParameterType != null -> createErrorTypeRef(
                        superTypeRef.source,
                        "Type parameter cannot be used as a supertype",
                        delegatedTypeRef = transformed,
                    )

                    transformed !is CfirResolvedTypeRef -> createErrorTypeRef(
                        superTypeRef.source,
                        "Unresolved super-type: ${superTypeRef.renderReadable()}",
                        DiagnosticKind.UnresolvedSupertype,
                    )

                    else -> transformed
                }
            }
        }
    }

    /**
     * class-like 超类型解析的公共执行骨架。
     *
     * 负责计算状态检查、递归环占位、类型解析配置构造和隐式 std.core 父类型补齐。
     */
    private fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        resolveSuperTypeRefs: (CfirSpecificTypeResolverTransformer, CfirTypeResolutionConfiguration) -> List<CfirResolvedTypeRef>,
    ): List<CfirResolvedTypeRef> {
        supertypeComputationSession.rememberDeclaration(classLikeDeclaration)
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)) {
            is SupertypeComputationStatus.Computed -> return status.supertypeRefs
            is SupertypeComputationStatus.Computing -> {
                return listOf(
                    createErrorTypeRef(
                        classLikeDeclaration.source,
                        classLikeDeclaration.name.asString(),
                        if (classLikeDeclaration is CfirTypeAlias) {
                            DiagnosticKind.RecursiveTypealiasExpansion
                        } else {
                            DiagnosticKind.LoopInSupertype
                        },
                    )
                )
            }

            SupertypeComputationStatus.NotComputed -> Unit
        }

        supertypeComputationSession.startComputingSupertypes(classLikeDeclaration)
        val resolvedTypeRefs = resolveSuperTypeRefs(
            CfirSpecificTypeResolverTransformer(
                session = session,
                supertypeSupplier = supertypeComputationSession.supertypesSupplier,
                expandTypeAliases = false,
            ),
            createTypeResolutionConfiguration(classLikeDeclaration, prepareScopes(classLikeDeclaration)),
        ).map { ref ->
            if (ref.coneType is ConeErrorType) ref.toErrorTypeRef() else ref
        }.let { resolvedRefs ->
            resolvedRefs.withImplicitStdCoreSupertypes(classLikeDeclaration, session)
        }
        supertypeComputationSession.storeSupertypes(classLikeDeclaration, resolvedTypeRefs)
        return resolvedTypeRefs
    }

    /** 构造 class-like 超类型解析所需的类型解析配置。 */
    @OptIn(PrivateForInline::class)
    private fun createTypeResolutionConfiguration(
        classLikeDeclaration: CfirClassLikeDeclaration,
        scopes: ScopePersistentList,
    ): CfirTypeResolutionConfiguration {
        return CfirTypeResolutionConfiguration(
            scopes = scopes,
            containingClassDeclarations = classDeclarationsStack.filterIsInstance<CfirClass>(),
            useSiteFile = useSiteFile,
        ).withAdditionalTypeParameters(classLikeDeclaration.typeParametersForResolution())
    }

}

/**
 * 在“非标准库编译”模式下，源码里未显式写出的标准父类型需要在此阶段补齐。
 *
 * 设计约束：
 * 1. 规则只用于 `std.core` 自举，避免把普通源码声明整体改写成显式 `Object/Any` 继承图；
 * 2. 只补“缺失的槽位”，不覆盖用户已经显式声明的继承关系；
 * 3. class 默认补直接父 class：普通 class -> Object，Object -> Any，Any -> 无；
 * 4. interface / struct / enum 在完全没有显式父类型时默认补 Any，但 Any 自身不能补成 `Any : Any` 自环。
 */
private fun List<CfirResolvedTypeRef>.withImplicitStdCoreSupertypes(
    declaration: CfirClassLikeDeclaration,
    session: CfirSession,
): List<CfirResolvedTypeRef> {
    if (session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) return this
    if (declaration.symbol.classId.packageFqName != StandardNames.FqNames.core) return this

    val implicitRefs = when (declaration) {
        is CfirClass -> declaration.implicitClassSupertypes(this)
        is CfirInterface, is CfirStruct, is CfirEnum ->
            if (declaration.symbol.classId != StdlibClassIds.Any && isEmpty()) {
                listOf(implicitStdCoreTypeRef(StdlibClassIds.Any))
            } else {
                emptyList()
            }
        else -> emptyList()
    }

    if (implicitRefs.isEmpty()) return this
    return this + implicitRefs
}

/** 计算 std.core class 在源码未显式写出时需要补齐的默认父类型。 */
private fun CfirClass.implicitClassSupertypes(
    resolvedDirectSuperTypes: List<CfirResolvedTypeRef>,
): List<CfirResolvedTypeRef> {
    val classId = symbol.classId
    if (classId == StdlibClassIds.Any) return emptyList()

    val hasExplicitConcreteSuper = resolvedDirectSuperTypes
        .map(CfirResolvedTypeRef::coneType)
        .any(ConeCangJieType::isConcreteSuperClassifier)

    if (hasExplicitConcreteSuper) return emptyList()

    return when (classId) {
        StdlibClassIds.Object -> listOf(implicitStdCoreTypeRef(StdlibClassIds.Any))
        else -> listOf(implicitStdCoreTypeRef(StdlibClassIds.Object))
    }
}

/** 判断 cone type 是否表示 class 可继承槽位中的具体父类型。 */
private fun ConeCangJieType.isConcreteSuperClassifier(): Boolean = when (this) {
    is ConeClassLikeType -> !isInterface
    is ConeStructType, is ConeEnumType -> true
    else -> false
}

/** 构造 std.core 隐式父类型对应的 resolved typeRef。 */
private fun implicitStdCoreTypeRef(classId: ClassId): CfirResolvedTypeRef {
    val implicitType = when (classId) {
        // `std.core.Any` 是根接口，补图时必须保留 interface 身份，
        // 否则后续“具体父类”判定会把它误当成 class。
        StdlibClassIds.Any -> ConeClassLikeType(classId.toLookupTag(), isInterface = true)
        else -> ConeClassLikeType(classId.toLookupTag())
    }
    return implicitType.toCfirResolvedTypeRef()
}

/**
 * 将 [SupertypeComputationSession] 中的解析结果写回 CFIR 树。
 *
 * 该 transformer 不重新解析类型，只替换未解析超类型、发布 SUPER_TYPES phase，
 * 并把已解析声明边记录到 super type graph store。
 */
private class CfirApplySupertypesTransformer(
    /** 当前阶段共享的超类型计算缓存。 */
    private val supertypeComputationSession: SupertypeComputationSession,
    /** 写回与图记录使用的会话。 */
    private val session: CfirSession,
    /** 保留给与其它 phase transformer 构造签名对齐的 scope session。 */
    private val scopeSession: ScopeSession,
) : CfirDefaultTransformer<Any?>() {
    /** 不属于 SUPER_TYPES 写回范围的元素保持原样。 */
    override fun <E : CfirElement> transformElement(element: E, data: Any?): E = element

    /** 继续转换声明内容的子节点。 */
    private fun transformDeclarationContent(declaration: CfirDeclaration, data: Any?): CfirDeclaration {
        @Suppress("UNCHECKED_CAST")
        return declaration.transformChildren(this, data) as CfirDeclaration
    }

    /** 在文件异常包装下回写文件内所有声明。 */
    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        return withFileAnalysisExceptionWrapping(file) {
            transformDeclarationContent(file, null) as CfirFile
        }
    }

    /** 回写 class 超类型并发布 SUPER_TYPES phase。 */
    override fun transformClass(klass: CfirClass, data: Any?): CfirClass {
        applyResolvedSupertypesToClassLike(klass)
        klass.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(klass, null) as CfirClass
    }

    /** 回写 interface 超类型并发布 SUPER_TYPES phase。 */
    override fun transformInterface(`interface`: CfirInterface, data: Any?): CfirInterface {
        applyResolvedSupertypesToClassLike(`interface`)
        `interface`.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(`interface`, null) as CfirInterface
    }

    /** 回写 struct 超类型并发布 SUPER_TYPES phase。 */
    override fun transformStruct(struct: CfirStruct, data: Any?): CfirStruct {
        applyResolvedSupertypesToClassLike(struct)
        struct.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(struct, null) as CfirStruct
    }

    /** 回写 enum 超类型并发布 SUPER_TYPES phase。 */
    override fun transformEnum(enum: CfirEnum, data: Any?): CfirEnum {
        applyResolvedSupertypesToClassLike(enum)
        enum.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(enum, null) as CfirEnum
    }

    /** 发布 extend 的 SUPER_TYPES phase，并继续遍历其成员。 */
    override fun transformExtend(extend: CfirExtend, data: Any?): CfirExtend {
        extend.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(extend, null) as CfirExtend
    }

    /** 回写 typealias expanded type 并发布 SUPER_TYPES phase。 */
    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Any?): CfirTypeAlias {
        if (typeAlias.expandedTypeRef !is CfirResolvedTypeRef) {
            val expanded = supertypeComputationSession.expandTypealiasInPlace(
                supertypeComputationSession.getResolvedExpandedTypeRef(typeAlias),
                session,
            )
            typeAlias.replaceExpandedTypeRef(expanded)
        }
        typeAlias.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(typeAlias, null) as CfirTypeAlias
    }

    /** 将 class-like 的已解析超类型写回声明并记录到超类型图。 */
    private fun applyResolvedSupertypesToClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        val resolvedRefs = supertypeComputationSession.getResolvedSupertypeRefs(classLikeDeclaration)
            .map { ref -> supertypeComputationSession.expandTypealiasInPlace(ref, session) }
        if (classLikeDeclaration.superTypeRefs.any { it !is CfirResolvedTypeRef }) {
            classLikeDeclaration.replaceSuperTypeRefs(resolvedRefs)
        }
        recordResolvedSupertypes(classLikeDeclaration, resolvedRefs)
    }

    /** 把声明的直接超类型边写入 super type graph store。 */
    private fun recordResolvedSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        resolvedRefs: List<CfirResolvedTypeRef>,
    ) {
        val graphStore = session.superTypeGraphStoreOrNull ?: return
        graphStore.recordDeclared(
            classLikeDeclaration,
            resolvedRefs.mapNotNull { ref ->
                if (!ref.isSemanticallyValidDeclaredSupertypeEdge(classLikeDeclaration, session)) {
                    return@mapNotNull null
                }
                CfirSuperTypeGraphEdge(
                    typeRef = ref,
                    renderedType = ref.renderReadable(),
                    resolvedClassSymbol = ref.toReferencedDeclaration(session)?.symbol
                        as? org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>,
                )
            },
        )
    }
}

/**
 * 判断声明超类型是否能进入继承图。
 *
 * 非法继承关系仍由 declaration checker 负责报告；继承图只保留语义上有效的边，
 * 避免错误恢复阶段把 `interface <: class` 这类已非法的关系再解释成继承环。
 */
private fun CfirResolvedTypeRef.isSemanticallyValidDeclaredSupertypeEdge(
    owner: CfirClassLikeDeclaration,
    session: CfirSession,
): Boolean {
    val semanticType = classifyDeclaredSupertype(session).ordinarySupertypeTypeOrNull() ?: return false
    val target = semanticType.toReferencedDeclaration(session) ?: return false
    if (owner is CfirInterface && target !is CfirInterface) return false
    return true
}

/**
 * SUPER_TYPES 前置阶段的 extend 超类型边采集器。
 *
 * 它只解析 extend 的目标类型和接口父类型，并把合法边写入图存储；
 * 正式声明树的超类型写回仍由 SUPER_TYPES 主阶段完成。
 */
private class CfirExtendSupertypesCollector(
    /** 当前采集使用的会话。 */
    private val session: CfirSession,
    /** 当前采集共享的 scope 缓存会话。 */
    private val scopeSession: ScopeSession,
    /** extend 超类型边写入的图存储。 */
    private val graphStore: org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore,
) : CfirDefaultVisitor<Unit, CfirTypeResolutionConfiguration>() {
    /** extend 类型引用解析器。 */
    private val typeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    /** 当前遍历路径上的 class-like 容器栈。 */
    private val classDeclarationsStack: ArrayDeque<CfirClassLikeDeclaration> = ArrayDeque()
    /** 当前采集所在的 use-site 文件。 */
    private var useSiteFile: CfirFile? = null

    /** 采集一组文件内所有 extend 声明形成的有效超类型边。 */
    fun collect(files: List<CfirFile>) {
        files.forEach { file ->
            val configuration = CfirTypeResolutionConfiguration.EMPTY
                .withUseSiteFile(file)
                // extend 超类型采集与正式 SUPER_TYPES 解析必须共享同一套作用域优先级，
                // 否则索引阶段会先把本地类型错误解析成默认导入类型，后续所有 extend 规则都会被污染。
                .withScopes(createImportingScopes(file, session).toPersistentList())
            withFile(file) {
                file.declarations.forEach { declaration ->
                    declaration.accept(this, configuration)
                }
            }
        }
    }

    /** 默认不处理普通元素。 */
    override fun visitElement(element: CfirElement, data: CfirTypeResolutionConfiguration) = Unit

    /** 进入 class 容器并继续采集其嵌套 extend。 */
    override fun visitClass(klass: CfirClass, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(klass, data)
    }

    /** 进入 interface 容器并继续采集其嵌套 extend。 */
    override fun visitInterface(`interface`: CfirInterface, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(`interface`, data)
    }

    /** 进入 struct 容器并继续采集其嵌套 extend。 */
    override fun visitStruct(struct: CfirStruct, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(struct, data)
    }

    /** 进入 enum 容器并继续采集其嵌套 extend。 */
    override fun visitEnum(enum: CfirEnum, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(enum, data)
    }

    /** 解析并记录单个 extend 声明贡献的接口超类型边。 */
    override fun visitExtend(extend: CfirExtend, data: CfirTypeResolutionConfiguration) {
        val configuration = data
            .withUseSiteFile(useSiteFile ?: data.useSiteFile ?: return)
            .withTopContainer(extend)
            .withContainingClassDeclarations(classDeclarationsStack.filterIsInstance<CfirClass>())
            .withAdditionalTypeParameters(extend.typeParameters)

        extend.transformExtendedTypeRef(typeResolverTransformer, configuration)
        extend.transformSuperTypeRefs(typeResolverTransformer, configuration)

        val extendedTypeRef = extend.extendedTypeRef as? CfirResolvedTypeRef ?: return
        if (!extendedTypeRef.isLegalExtendTargetForSupertypeGraph(session)) return

        val ownerClassId = extendedTypeRef.coneType.classIdOrPrimitiveClassId ?: return
        val edges = extend.superTypeRefs.mapNotNull { ref ->
            val resolvedRef = ref as? CfirResolvedTypeRef ?: return@mapNotNull null
            if (!resolvedRef.isValidExtendInterfaceSupertypeForGraph(session)) return@mapNotNull null
            CfirSuperTypeGraphEdge(
                typeRef = resolvedRef,
                renderedType = resolvedRef.renderReadable(),
                resolvedClassSymbol = resolvedRef.toReferencedDeclaration(session)?.symbol
                    as? org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>,
                sourceExtend = extend,
            )
        }
        if (edges.isEmpty()) return
        graphStore.recordExtended(ownerClassId, edges)
    }

    /** 在嵌套 class-like 容器上下文中继续访问子声明。 */
    private fun visitNestedContainer(
        declaration: CfirClassLikeDeclaration,
        data: CfirTypeResolutionConfiguration,
    ) {
        val configuration = data.withAdditionalTypeParameters(declaration.typeParametersForResolution())
        classDeclarationsStack.addLast(declaration)
        try {
            declaration.declarations.forEach { child ->
                child.accept(this, configuration)
            }
        } finally {
            classDeclarationsStack.removeLast()
        }
    }

    /** 在指定文件上下文中执行采集逻辑，并在结束后恢复旧文件。 */
    private inline fun withFile(file: CfirFile, block: () -> Unit) {
        val previous = useSiteFile
        useSiteFile = file
        try {
            block()
        } finally {
            useSiteFile = previous
        }
    }
}

/**
 * 构造 supertype 类型解析使用的文件导入 scope 列表。
 *
 * 返回顺序遵循 [CfirTypeResolutionConfiguration] 的高优先级在前契约。
 */
private fun createImportingScopes(file: CfirFile, session: CfirSession): List<CfirScope> {
    val symbolProvider: CfirSymbolProvider = session.symbolProvider
    val imports = file.imports
    val resolvedImports = session.importBindingStoreOrNull?.getBindings(file)?.imports
    val defaultImports = session.defaultImportsProvider
        .getDefaultImports(includeLowPriorityImports = true)
        .filter { it.fqName !in session.defaultImportsProvider.excludedImports }
        .map(ImportPath::toImport)

    return buildList {
        // CfirTypeResolver 按顺序查找 scope；supertype 解析同样必须高优先级在前。
        add(CfirFileDeclaredTopLevelScope(file))
        add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
        add(CfirExplicitSimpleImportingScope(imports, symbolProvider, resolvedImports))
        add(CfirExplicitStarImportingScope(imports, symbolProvider, resolvedImports))
        add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
        add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
    }
}

/** 把导入路径转换成 CFIR import 节点，供导入 scope 复用。 */
private fun ImportPath.toImport() = buildImport {
    source = null
    importedFqName = fqName
    isAllUnder = this@toImport.isAllUnder
    aliasName = alias
    aliasSource = null
}

/** 为 class-like 自身类型参数构造局部类型参数 scope。 */
private fun CfirClassLikeDeclaration.typeParametersScope(): CfirScope? {
    val typeParameters = typeParametersForResolution()
    if (typeParameters.isEmpty()) return null
    return CfirTypeParameterScopeImpl(typeParameters)
}

/** 返回 class-like 声明在超类型解析中可见的自身类型参数。 */
private fun CfirClassLikeDeclaration.typeParametersForResolution(): List<CfirTypeParameter> = when (this) {
    is CfirClass -> typeParameters
    is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
    is CfirInterface -> typeParameters
    is CfirStruct -> typeParameters
    is CfirEnum -> typeParameters
    is CfirTypeAlias -> typeParameters
}

/** 用已解析类型引用替换 class-like 声明的 superTypeRefs。 */
private fun CfirClassLikeDeclaration.replaceSuperTypeRefs(newRefs: List<CfirTypeRef>) {
    when (this) {
        is CfirClassImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        is CfirInterfaceImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        is CfirStructImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        is CfirEnumImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        else -> Unit
    }
}

/** 在 extend 声明中把指定旧 supertype ref 替换成新 ref。 */
private fun CfirExtend.replaceSuperTypeRef(oldRef: CfirTypeRef, newRef: CfirTypeRef) {
    when (this) {
        is CfirExtendImpl -> superTypeRefs.replaceAll { ref -> if (ref === oldRef) newRef else ref }
        else -> Unit
    }
}

/** 继承图 DFS 的访问状态。 */
private enum class InheritanceVisitStatus {
    /** 当前节点正在递归栈上。 */
    Visiting,
    /** 当前节点及其依赖已经访问完成。 */
    Visited,
}

/** 从 resolved typeRef 解析出它引用的 class-like 声明。 */
private fun CfirResolvedTypeRef.toReferencedDeclaration(session: CfirSession): CfirClassLikeDeclaration? =
    coneType.toReferencedDeclaration(session)

/** 从 cone type 解析出它引用的 class-like 声明。 */
private fun ConeCangJieType.toReferencedDeclaration(session: CfirSession): CfirClassLikeDeclaration? {
    val classId = when (this) {
        is ConePrimitiveType -> kind.classId
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    } ?: return null
    return session.cfirProvider.getCfirClassifierByFqName(classId)
        ?: session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

/**
 * 判断 extend 的目标类型是否可以作为超类型图 owner。
 *
 * 只有具体 class / struct / enum / primitive 目标参与 extend 超类型图，接口和复合类型不作为 owner。
 */
private fun CfirResolvedTypeRef.isLegalExtendTargetForSupertypeGraph(session: CfirSession): Boolean {
    if (coneType is ConeErrorType) return false
    return when (val expandedType = coneType.fullyExpandedTypeUsingAbbreviation(session)) {
        is ConeErrorType -> false
        is ConeClassLikeType -> !expandedType.isInterface
        is ConeStructType,
        is ConeEnumType,
        is ConePrimitiveType -> true
        is ConeFunctionType,
        is ConeTupleType,
        is ConeVArrayType,
        is ConeIntersectionType,
        is ConeUnionType -> false
        else -> false
    }
}

/** 判断 extend 声明中的 supertype 是否是可记录到图中的接口类型。 */
private fun CfirResolvedTypeRef.isValidExtendInterfaceSupertypeForGraph(session: CfirSession): Boolean {
    if (coneType is ConeErrorType) return false
    val expandedType = coneType.fullyExpandedTypeUsingAbbreviation(session)
    return expandedType is ConeClassLikeType && expandedType.isInterface
}

/** 构造 SUPER_TYPES 阶段使用的错误 resolved typeRef。 */
private fun createErrorTypeRef(
    sourceElement: CjSourceElement?,
    message: String,
    kind: DiagnosticKind = DiagnosticKind.Other,
    delegatedTypeRef: CfirTypeRef? = null,
): CfirResolvedTypeRef = buildErrorTypeRef {
    source = sourceElement

    diagnostic = ConeSimpleDiagnostic(message, kind)
    this.delegatedTypeRef = delegatedTypeRef
}

/** 把携带 [ConeErrorType] 的普通 resolved typeRef 规范化为 error typeRef。 */
private fun CfirResolvedTypeRef.toErrorTypeRef(): CfirResolvedTypeRef {
    val errorType = coneType as? ConeErrorType ?: return this
    return buildErrorTypeRef {
        source = this@toErrorTypeRef.source
        coneType = errorType
        annotations += this@toErrorTypeRef.annotations
        delegatedTypeRef = this@toErrorTypeRef.delegatedTypeRef ?: this@toErrorTypeRef
        diagnostic = errorType.diagnostic
    }
}

/** 返回类型引用在诊断和图记录中使用的可读文本。 */
private fun CfirTypeRef.renderReadable(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.toString()
    else -> toString()
}

/** 在持久列表头部加入单个元素，保持 scope 优先级语义。 */
private fun <E> PersistentList<E>.push(element: E): PersistentList<E> = add(0, element)
/** 在持久列表头部加入一组元素，保持 scope 优先级语义。 */
private fun <E> PersistentList<E>.pushAll(collection: Collection<E>): PersistentList<E> = addAll(0, collection)
/** scope 非空时压入列表头部，否则保持原列表。 */
private fun ScopePersistentList.pushIfNotNull(scope: CfirScope?): ScopePersistentList = if (scope == null) this else push(scope)
