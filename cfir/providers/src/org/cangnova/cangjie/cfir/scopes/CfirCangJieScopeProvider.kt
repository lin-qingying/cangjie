package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionCacheOwner
import org.cangnova.cangjie.cfir.ScopeSessionOwnedId
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.resolve.expandedConeTypeWithEnsuredPhase
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationInheritanceInput
import org.cangnova.cangjie.cfir.resolve.isReferenceCycleTarget
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeSources
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeInputStamp
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationPackage
import org.cangnova.cangjie.cfir.resolve.providers.ordinaryMemberSupertypeOrNull
import org.cangnova.cangjie.cfir.resolve.providers.scopeTraversalTypeOrNull
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirCjmpMergedDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.provenanceToConcreteSubstitutor
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResultForSpecific
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirScopeWithCallableCopyReturnTypeUpdater
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirCjmpMemberScopeView
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirMemberInstantiationInput
import org.cangnova.cangjie.cfir.scopes.impl.CfirPreparedMemberSubstitution
import org.cangnova.cangjie.cfir.scopes.impl.captureInheritanceMemberHeader
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirCompositeTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirSupertypePath
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.TypeAliasConstructorsSubstitutingScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirCjmpMemberSnapshot
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseWithCallableMembers
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseWithCallableMembersInSupertypes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.declarationSelfType
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.name.FqName

/**
 * 同一稳定来源分区中一个完成代的证明。来源观察只读已发布版本，不执行解析回调。
 *
 * token 按身份比较，同一 owner 的两个不同代不能在依赖合并时互相覆盖。
 */
internal class CfirScopeCacheEpoch(
    val owner: ScopeSessionCacheOwner,
    val generation: Long,
    private val sourceIsCurrent: () -> Boolean,
) {
    fun isCurrent(): Boolean = owner.generation == generation && sourceIsCurrent()
}

/**
 * 已准备输入或缓存产品及其全部真实依赖。外围 wrapper 必须转发基础产品的证明，
 * 不能把任意旧 scope 包装为当前 owner 的新产品。
 */
internal data class CfirPreparedScopeProduct<out PRODUCT : Any>(
    val value: PRODUCT,
    val dependencies: Set<CfirScopeCacheEpoch>,
) {
    fun isCurrent(): Boolean = dependencies.all(CfirScopeCacheEpoch::isCurrent)
}

/**
 * 仓颉语言的 scope 提供者，对标 K2 FirKotlinScopeProvider。
 */
open class CfirCangJieScopeProvider : CfirScopeProvider(), CfirSessionComponent {
    /**
     * 泛型实例化检查使用的一对声明成员图和实例化 use-site 成员图。
     *
     * scope 的创建与 substitution 留在 providers 层，checker 只通过这组结果读取继承事实。
     */
    data class GenericInstantiationMemberScopes(
        val raw: CfirClassUseSiteMemberScope,
        val substituted: CfirClassSubstitutionScope,
    )

    /** 为泛型实例化检查创建统一的原始成员图及其 use-site substitution 视图。 */
    fun getGenericInstantiationMemberScopes(
        classSymbol: CfirClassLikeSymbol<*>,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
        instantiatedType: ConeCangJieType,
        inheritanceProvenanceType: ConeCangJieType,
        scopeOwner: CfirMemberScopeOwner,
        memberScopeView: CfirCjmpMemberScopeView,
    ): GenericInstantiationMemberScopes {
        fun prepareRaw(): CfirClassUseSiteMemberScope = getUseSiteMemberScope(
            useSiteSession = useSiteSession,
            classSymbol = classSymbol,
            scopeSession = scopeSession,
            symbolProvider = useSiteSession.symbolProvider,
            extendProvider = if (scopeOwner.includesExtendScopes) useSiteSession.extendProvider else null,
            directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
            ownerType = instantiatedType,
            inheritanceProvenanceOwnerType = inheritanceProvenanceType,
            dispatchReceiverType = instantiatedType,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
            scopeOwner = scopeOwner,
            memberScopeView = memberScopeView,
            memberRequiredPhase = if (memberScopeView != CfirCjmpMemberScopeView.RAW) CfirResolvePhase.CJMP_MATCHING
                else CfirResolvePhase.STATUS,
        )
        return cacheWithPreparedInputs(
            useSiteSession, scopeSession, GENERIC_INSTANTIATION_MEMBER_SCOPES,
            prepare = {
                val raw = prepareRaw()
                val substituted = getClassSubstitutionScope(useSiteSession, scopeSession, ::prepareRaw, instantiatedType)
                CfirPreparedScopeProduct(raw to substituted.value, raw.cacheDependencies + substituted.dependencies)
            },
            create = { (raw, substituted) -> GenericInstantiationMemberScopes(raw, substituted) },
        ).value
    }

    /**
     * 在给定 resolve 阶段读取 class-like 的 use-site 成员 scope。
     *
     * 构造器调用、委托调用和普通成员解析必须从同一声明视图取候选；CJMP matching 前保留
     * 原始声明，matching 后的 specific 查询才使用 provider 装配的 common/specific 合并图。
     */
    fun getUseSiteMemberScope(
        classSymbol: CfirClassLikeSymbol<*>,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
        ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
        dispatchReceiverType: ConeCangJieType? = ownerType,
        scopeKind: CfirClassMemberScopeKind,
        memberRequiredPhase: CfirResolvePhase?,
        symbolProvider: CfirSymbolProvider = useSiteSession.symbolProvider,
        extendProvider: CfirExtendProvider? = useSiteSession.extendProvider,
        directSupertypeProvider: CfirDirectSupertypeProvider? = useSiteSession.directSupertypeProviderOrNull,
        inheritanceProvenanceOwnerType: ConeCangJieType? = declarationSelfType(classSymbol),
        allowBareGenericStaticQualifierExtends: Boolean = false,
        excludingExtend: CfirExtend? = null,
        scopeOwner: CfirMemberScopeOwner = CfirMemberScopeOwner.UseSite,
        memberScopeView: CfirCjmpMemberScopeView = useSiteMemberScopeView(useSiteSession, memberRequiredPhase),
    ): CfirClassUseSiteMemberScope {
        return getUseSiteMemberScope(
            useSiteSession = useSiteSession,
            classSymbol = classSymbol,
            scopeSession = scopeSession,
            symbolProvider = symbolProvider,
            extendProvider = extendProvider,
            directSupertypeProvider = directSupertypeProvider,
            ownerType = ownerType,
            inheritanceProvenanceOwnerType = inheritanceProvenanceOwnerType,
            dispatchReceiverType = dispatchReceiverType,
            scopeKind = scopeKind,
            allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
            excludingExtend = excludingExtend,
            inheritedLookupProvenance = CfirCallableLookupProvenance.None,
            supertypePath = CfirSupertypePath.root(classSymbol),
            scopeOwner = scopeOwner,
            memberScopeView = memberScopeView,
            memberRequiredPhase = memberRequiredPhase,
        )
    }

    /** 父 scope 生产保留来源边和 owner 视角，阶段保障仍由同一 provider 完成。 */
    internal fun getUseSiteMemberScope(
        classSymbol: CfirClassLikeSymbol<*>,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
        symbolProvider: CfirSymbolProvider,
        extendProvider: CfirExtendProvider?,
        directSupertypeProvider: CfirDirectSupertypeProvider?,
        ownerType: ConeCangJieType?,
        inheritanceProvenanceOwnerType: ConeCangJieType?,
        dispatchReceiverType: ConeCangJieType?,
        scopeKind: CfirClassMemberScopeKind,
        allowBareGenericStaticQualifierExtends: Boolean,
        excludingExtend: CfirExtend?,
        inheritedLookupProvenance: CfirCallableLookupProvenance,
        supertypePath: CfirSupertypePath,
        scopeOwner: CfirMemberScopeOwner,
        memberScopeView: CfirCjmpMemberScopeView,
        memberRequiredPhase: CfirResolvePhase?,
    ): CfirClassUseSiteMemberScope {
        return withScopeConstruction {
            val sources = inputSources(useSiteSession, symbolProvider, extendProvider, directSupertypeProvider)
            // 原 self/type constructor 由声明 symbol 决定；scope 的实际 owner 已由
            // request.symbol 和 sources 保存，不能把普通 tag 升级为另一种结构身份。
            val request = ClassScopeRequest(
                classSymbol, ownerType, inheritanceProvenanceOwnerType, dispatchReceiverType, scopeKind,
                allowBareGenericStaticQualifierExtends, excludingExtend, inheritedLookupProvenance, scopeOwner, memberScopeView,
            )
            prepareUseSiteScope(sources, scopeSession, request, supertypePath, memberRequiredPhase)
        }
    }

    /** 来源身份、完成版本及 owned storage 由同一个 provider 分区管理。 */
    private data class ScopeInputSources(
        val session: CfirSession,
        val symbols: CfirSymbolProvider,
        val extends: CfirExtendProvider?,
        val directSupertypes: CfirDirectSupertypeProvider?,
        val typeAwareSupertypes: CfirTypeAwareSupertypeProvider?,
        val typeAwareSources: CfirTypeAwareSupertypeSources?,
    )

    private data class ScopePartitionKey(val producer: CfirCangJieScopeProvider, val sources: ScopeInputSources)
    private data class ScopeInputRevision(
        val extendRevision: Long?,
        val graphRevision: Long?,
        val typeAwareStamp: CfirTypeAwareSupertypeInputStamp?,
        val canonicalSources: CfirTypeAwareSupertypeSources,
        val canonicalExtendRevision: Long?,
        val canonicalGraphRevision: Long?,
        val canonicalTypeAwareProvider: CfirTypeAwareSupertypeProvider?,
        val canonicalTypeAwareStamp: CfirTypeAwareSupertypeInputStamp?,
    )
    private data class DeclaredSlot(val symbol: CfirClassLikeSymbol<*>, val view: CfirCjmpMemberScopeView)
    private data class DeclaredInputSignature(
        val slot: DeclaredSlot,
        val snapshot: CfirCjmpMemberSnapshot?,
        val physicalMembers: List<CfirDeclaration>,
        val superTypeRefs: List<CfirTypeRef>,
        val parameters: List<Pair<CfirTypeParameterRef, List<CfirTypeRef>>>,
    )
    private data class DeclaredScopeInput(
        val signature: DeclaredInputSignature,
        val declaredScope: CfirClassDeclaredMemberScope,
        val memberDeclarations: List<CfirDeclaration>,
    )

    private class ScopeCachePartition(
        val owner: ScopeSessionCacheOwner,
        var revision: ScopeInputRevision,
        var epoch: CfirScopeCacheEpoch,
    ) {
        val declarations = hashMapOf<DeclaredSlot, DeclaredInputSignature>()
        val aliases = hashMapOf<Pair<CfirTypeAliasSymbol, CfirCjmpMemberScopeView>, Pair<CfirTypeRef, ConeLookupTagBasedType>>()
        val extensions = hashMapOf<ExtendScopeInput, CfirExtendMemberScope.ScopeInput>()
        val instantiationHeaders = hashMapOf<DeclaredSlot, Map<CfirCallableSymbol<*>, CfirInheritanceMemberHeader>>()
        val dependencies = linkedSetOf<CfirScopeCacheEpoch>()
    }

    /** 完整查询语境不包含构造检查 path 或普通 requiredPhase 分区。 */
    private data class ClassScopeRequest(
        val symbol: CfirClassLikeSymbol<*>,
        val ownerType: ConeCangJieType?,
        val provenanceType: ConeCangJieType?,
        val dispatchType: ConeCangJieType?,
        val kind: CfirClassMemberScopeKind,
        val bareStaticExtends: Boolean,
        val excludedExtend: CfirExtend?,
        val lookupProvenance: CfirCallableLookupProvenance,
        val owner: CfirMemberScopeOwner,
        val view: CfirCjmpMemberScopeView,
    )

    private data class ClassScopeInput(
        val request: ClassScopeRequest,
        val declaredInput: DeclaredScopeInput,
        val parents: List<Pair<CfirInstantiatedSupertypeDescriptor, CfirTypeScope>>,
        val extendScope: CfirExtendMemberScope?,
        val memberInput: CfirMemberInstantiationInput,
        val dependencies: Set<CfirScopeCacheEpoch>,
    )

    /** 单次构造的 memo 只避免菱形重复遍历；不作为跨请求的完成产品缓存。 */
    private data class ScopePreparationKey(
        val owner: ScopeSessionCacheOwner,
        val generation: Long,
        val request: ClassScopeRequest,
        val requiredPhase: CfirResolvePhase?,
        val declaredInput: DeclaredScopeInput,
        val ownExtendScope: CfirExtendMemberScope?,
        val memberInput: CfirMemberInstantiationInput,
    )

    private class ScopeConstruction {
        val completed = hashMapOf<ScopePreparationKey, CfirClassUseSiteMemberScope>()
        val active = linkedSetOf<ScopePreparationKey>()
    }

    private inline fun <T> withScopeConstruction(action: () -> T): T {
        if (construction.get() != null) return action()
        construction.set(ScopeConstruction())
        return try {
            action()
        } finally {
            construction.remove()
        }
    }

    private fun inputSources(
        session: CfirSession,
        symbols: CfirSymbolProvider = session.symbolProvider,
        extends: CfirExtendProvider? = session.extendProviderOrNull,
        directSupertypes: CfirDirectSupertypeProvider? = session.directSupertypeProviderOrNull,
    ): ScopeInputSources {
        val candidate = session.typeAwareSupertypeProviderOrNull
        val typedSources = candidate?.inputStamp?.sources
        val structuralSources = typedSources?.let {
            CfirTypeAwareSupertypeSources(session, symbols, extends ?: it.extends, directSupertypes ?: it.directSupertypes)
        }
        val typeAware = if (candidate == null || structuralSources == null) null else candidate.forSources(structuralSources)
        // null 只禁止直接 extend 成员；匹配的结构父图仍保留真实 extend 接口来源。
        return ScopeInputSources(session, symbols, extends, directSupertypes, typeAware, typeAware?.inputStamp?.sources)
    }

    private fun currentRevision(sources: ScopeInputSources): ScopeInputRevision {
        val session = sources.session
        val canonical = CfirTypeAwareSupertypeSources(session, session.symbolProvider, session.extendProviderOrNull, session.directSupertypeProviderOrNull)
        val canonicalProvider = session.typeAwareSupertypeProviderOrNull
        return ScopeInputRevision(
            sources.extends?.modificationCount, sources.directSupertypes?.modificationCount, sources.typeAwareSupertypes?.inputStamp,
            canonical, canonical.extends?.modificationCount, canonical.directSupertypes?.modificationCount,
            canonicalProvider, canonicalProvider?.inputStamp,
        )
    }

    private fun partition(scopeSession: ScopeSession, sources: ScopeInputSources): ScopeCachePartition =
        scopeSession.getOrBuild(ScopePartitionKey(this, sources), SCOPE_CACHE_PARTITION) {
            val owner = scopeSession.createCacheOwner()
            val revision = currentRevision(sources)
            ScopeCachePartition(owner, revision, cacheEpoch(owner, sources, revision))
        }

    private fun cacheEpoch(
        owner: ScopeSessionCacheOwner,
        sources: ScopeInputSources,
        revision: ScopeInputRevision,
    ): CfirScopeCacheEpoch = CfirScopeCacheEpoch(owner, owner.generation) { currentRevision(sources) == revision }

    private fun retirePartition(
        scopeSession: ScopeSession,
        sources: ScopeInputSources,
        partition: ScopeCachePartition,
        revision: ScopeInputRevision = currentRevision(sources),
    ) {
        scopeSession.retire(partition.owner)
        partition.declarations.clear()
        partition.dependencies.clear()
        partition.aliases.clear()
        partition.extensions.clear()
        partition.instantiationHeaders.clear()
        partition.revision = revision
        partition.epoch = cacheEpoch(partition.owner, sources, revision)
    }

    /** 每个稳定分区只保留当前完成代；旧 raw、外围 wrapper 和 phase stamp 一起退役。 */
    private fun refreshPartition(
        scopeSession: ScopeSession,
        sources: ScopeInputSources,
        partition: ScopeCachePartition,
    ): Long {
        val revision = currentRevision(sources)
        if (partition.revision != revision || partition.dependencies.any { !it.isCurrent() }) {
            retirePartition(scopeSession, sources, partition, revision)
        }
        return partition.owner.generation
    }

    private fun memberSnapshot(
        sources: ScopeInputSources,
        request: ClassScopeRequest,
    ): CfirCjmpMemberSnapshot? {
        val session = sources.session
        if (request.view == CfirCjmpMemberScopeView.RAW || !session.isCjmpSpecificCompilationEnabled()) return null
        val declaration = request.symbol.cfir
        if (!declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return null
        return if (request.view == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) {
            session.cjmpFamilyAggregate.matchingGroupFor(declaration)
        } else if (session.requiresDeferredCjmpFinalMatching()) {
            session.cjmpFamilyAggregate.structureFor(declaration)
        } else {
            session.cjmpFamilyAggregate.matchingGroupFor(declaration)
        }
    }

    /** 相同 symbol/view 的声明与快照产品共享；成员输入改变时不积累旧 snapshot 键。 */
    private fun declaredScopeInput(
        scopeSession: ScopeSession,
        sources: ScopeInputSources,
        partition: ScopeCachePartition,
        request: ClassScopeRequest,
    ): DeclaredScopeInput {
        val slot = DeclaredSlot(request.symbol, request.view)
        val signature = DeclaredInputSignature(
            slot, memberSnapshot(sources, request), request.symbol.cfir.declarations.toList(),
            declaredParentRefs(request.symbol).toList(),
            // 包含外层参数引用；上界属于引用的真实声明，不能只筛选当前类直接声明的参数。
            request.symbol.cfir.typeParameters.map { it to it.symbol.cfir.bounds.toList() },
        )
        val previous = partition.declarations[slot]
        if (previous != null && previous != signature) {
            retirePartition(scopeSession, sources, partition)
        }
        partition.declarations[slot] = signature
        return scopeSession.getOrBuild(partition.owner.id(signature), DECLARED_SCOPE_INPUT) {
            val family = signature.snapshot
            val baseMembers = if (family == null) signature.physicalMembers else when (request.view) {
                CfirCjmpMemberScopeView.RAW -> error("A RAW declared input cannot use a merged family")
                CfirCjmpMemberScopeView.CJMP_ACTUALIZED -> family.membersFor(request.symbol.cfir)
                CfirCjmpMemberScopeView.CJMP_INSTANTIATION -> family.mergedDeclarationsFor(request.symbol.cfir)
            }
            val baseSymbols = baseMembers.mapTo(hashSetOf()) { it.symbol }
            // 生成 ctor 是真实 owner 的后置产物，合入本次冻结物理输入，不在 scope 查询时读活动树。
            val generated = signature.physicalMembers.filter { member -> member is CfirConstructor &&
                (member.origin == CfirDeclarationOrigin.ImplicitDefault || member.origin == CfirDeclarationOrigin.Synthetic.ImplicitConstructor) &&
                member.symbol !in baseSymbols }
            val members = java.util.Collections.unmodifiableList(ArrayList(baseMembers + generated))
            val scope = if (family == null) {
                CfirClassDeclaredMemberScope(request.symbol, members)
            } else {
                CfirCjmpMergedDeclaredMemberScope(request.symbol, request.symbol.cfir, members)
            }
            DeclaredScopeInput(signature, scope, members)
        }
    }

    /**
     * 阶段、typed parents 与来源索引准备之后才发布重型节点。
     * 解析回调或父 builder 发布新代时重新准备，不返回旧 immutable 父布局。
     */
    private fun prepareUseSiteScope(
        initialSources: ScopeInputSources,
        scopeSession: ScopeSession,
        request: ClassScopeRequest,
        supertypePath: CfirSupertypePath,
        requiredPhase: CfirResolvePhase?,
    ): CfirClassUseSiteMemberScope {
        var sources = initialSources
        var partition = partition(scopeSession, sources)
        // 实例化表属于完成签名消费；RAW/普通结构视图保持调用方原阶段承诺。
        val memberRequiredPhase = if (request.view == CfirCjmpMemberScopeView.CJMP_INSTANTIATION)
            maxOf(requiredPhase ?: CfirResolvePhase.IMPLICIT_TYPES, CfirResolvePhase.IMPLICIT_TYPES) else requiredPhase
        while (true) {
            // null 只免除 callable member 阶段；父引用必须先完成正常 SUPER_TYPES 生命周期。
            request.symbol.lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
            val selectedSources = inputSources(sources.session, sources.symbols, sources.extends, sources.directSupertypes)
            if (selectedSources != sources) {
                sources = selectedSources
                partition = partition(scopeSession, sources)
            }
            val generation = refreshPartition(scopeSession, sources, partition)
            val stampId = partition.owner.id(request)
            val phaseStamp = scopeSession.getOrBuild(stampId, USE_SITE_REQUIRED_MEMBERS_PHASE) {
                CfirRequiredMembersPhaseStamp()
            }
            scopeSession.ensureRequiredMembersPhase(
                stampId, USE_SITE_REQUIRED_MEMBERS_PHASE, memberRequiredPhase,
                resolveOwner = { request.symbol.cfir.lazyResolveToPhaseWithCallableMembers(it) },
                resolveHierarchy = {
                    request.symbol.cfir.lazyResolveToPhaseWithCallableMembersInSupertypes(sources.session, it)
                },
            )
            if (refreshPartition(scopeSession, sources, partition) != generation) continue
            if (request.kind == CfirClassMemberScopeKind.USE_SITE && sources.extends != null && request.ownerType != null) {
                request.ownerType.expandedExtendTargetKey?.let { target ->
                    CfirExtendMemberScope.ensureRequiredMembersPhase(target, sources.extends, request.ownerType, memberRequiredPhase)
                }
            }
            if (refreshPartition(scopeSession, sources, partition) != generation) continue
            val declared = declaredScopeInput(scopeSession, sources, partition, request)
            if (refreshPartition(scopeSession, sources, partition) != generation) continue
            val ownExtend = ownExtendScope(scopeSession, sources, partition, request, declared, memberRequiredPhase)
            if (!ownExtend.isCurrent() || refreshPartition(scopeSession, sources, partition) != generation) continue
            if (declaredScopeInput(scopeSession, sources, partition, request) !== declared) continue
            if (refreshPartition(scopeSession, sources, partition) != generation) continue

            val headerDependencies = linkedSetOf(partition.epoch)
            val memberInput = prepareClassInstantiationInput(sources, scopeSession, partition, request, declared, headerDependencies)
            if (refreshPartition(scopeSession, sources, partition) != generation || headerDependencies.any { !it.isCurrent() }) continue
            val traversal = checkNotNull(construction.get())
            val preparation = ScopePreparationKey(partition.owner, generation, request, memberRequiredPhase, declared, ownExtend.value.scope, memberInput)
            traversal.completed[preparation]?.takeIf { scope ->
                scope.cacheGeneration == generation && scope.cacheDependencies.all(CfirScopeCacheEpoch::isCurrent)
            }?.let { return it }
            check(traversal.active.add(preparation)) {
                "Ordinary member scope inputs recurse before the completed supertype product has excluded the cycle"
            }
            try {
                val dependencies = linkedSetOf(partition.epoch).apply { addAll(ownExtend.dependencies); addAll(headerDependencies) }
                val parents = buildParentScopes(sources, request, scopeSession, supertypePath, memberRequiredPhase, dependencies)
                if (refreshPartition(scopeSession, sources, partition) != generation) continue
                val finalExtend = ownExtendScope(scopeSession, sources, partition, request, declared, memberRequiredPhase)
                if (!finalExtend.isCurrent() || finalExtend.value.scope !== ownExtend.value.scope) continue
                dependencies.addAll(finalExtend.dependencies)
                val extendScope = finalExtend.value.scope
                if (refreshPartition(scopeSession, sources, partition) != generation) continue
                if (declaredScopeInput(scopeSession, sources, partition, request) !== declared) continue
                if (refreshPartition(scopeSession, sources, partition) != generation) continue

                if (dependencies.any { !it.isCurrent() }) continue
                partition.dependencies.addAll(dependencies.filter { it.owner !== partition.owner })
                val finalHeaderInput = prepareClassInstantiationInput(sources, scopeSession, partition, request, declared, dependencies)
                if (finalHeaderInput != memberInput || refreshPartition(scopeSession, sources, partition) != generation) continue
                val input = ClassScopeInput(request, declared, parents, extendScope, memberInput, dependencies.toSet())
                val completed = scopeSession.getOrBuild(partition.owner.id(input), CLASS_USE_SITE_SCOPE) {
                    CfirClassUseSiteMemberScope(
                        session = sources.session,
                        classSymbol = request.symbol,
                        scopeSession = scopeSession,
                        symbolProvider = sources.symbols,
                        ownerType = request.ownerType,
                        inheritanceProvenanceOwnerType = request.provenanceType,
                        dispatchReceiverType = request.dispatchType,
                        scopeKind = request.kind,
                        allowBareGenericStaticQualifierExtends = request.bareStaticExtends,
                        excludingExtend = request.excludedExtend,
                        inheritedLookupProvenance = request.lookupProvenance,
                        scopeOwner = request.owner,
                        memberScopeView = request.view,
                        declaredScope = declared.declaredScope,
                        declarationMemberSnapshot = declared.signature.snapshot,
                        parentScopeInputs = parents,
                        extendScope = extendScope,
                        memberInstantiationInput = memberInput,
                        requiredMembersPhaseStamp = phaseStamp,
                        cacheOwner = partition.owner,
                        cacheGeneration = generation,
                        cacheDependencies = dependencies.toSet(),
                    )
                }
                if (refreshPartition(scopeSession, sources, partition) != generation) continue
                if (!completed.cacheDependencies.all(CfirScopeCacheEpoch::isCurrent)) continue
                traversal.completed[preparation] = completed
                return completed
            } finally {
                traversal.active.remove(preparation)
            }
        }
    }

    /** 原域 header 与 request 投影在 prepare 内完成；低阶段结构查询没有实例化产品。 */
    private fun prepareClassInstantiationInput(
        sources: ScopeInputSources,
        scopeSession: ScopeSession,
        partition: ScopeCachePartition,
        request: ClassScopeRequest,
        declared: DeclaredScopeInput,
        dependencies: MutableSet<CfirScopeCacheEpoch>,
    ): CfirMemberInstantiationInput {
        if (request.view != CfirCjmpMemberScopeView.CJMP_INSTANTIATION) return CfirMemberInstantiationInput.NotRequested
        val owner = request.symbol.cfir
        val family = declared.signature.snapshot
        val members = declared.memberDeclarations
        val aliasExpansion = scopeAliasExpansion(sources, scopeSession, dependencies,
            request.kind == CfirClassMemberScopeKind.USE_SITE && sources.directSupertypes != null &&
                sources.directSupertypes !== sources.session.directSupertypeProviderOrNull)
        fun normalize(type: ConeCangJieType): ConeCangJieType =
            type.fullyExpandedTypeUsingAbbreviation(sources.session, sources.symbols, aliasExpansion)
        fun registerSource(declaration: CfirDeclaration) {
            if (!declaration.origin.fromSource) return
            val ownerSources = inputSources(declaration.moduleData.session)
            val ownerPartition = partition(scopeSession, ownerSources)
            refreshPartition(scopeSession, ownerSources, ownerPartition)
            dependencies += ownerPartition.epoch
        }
        val signatureType = normalize(checkNotNull(request.provenanceType))
        val signatureSubstitutor = if (owner.typeParameters.isEmpty()) ConeSubstitutor.Empty else
            checkNotNull(createMemberDeclarationSubstitutor(request.symbol, signatureType, sources.session)) {
                "The completed instantiation owner requires its complete signature arguments"
            }
        val logicalValues = owner.typeParameters.associate { parameter ->
            parameter.symbol.toLookupTag() as TypeConstructorMarker to signatureSubstitutor.substituteOrSelf(parameter.symbol.constructType())
        }
        val headers = linkedMapOf<CfirCallableSymbol<*>, CfirInheritanceMemberHeader>()
        val substitutions = linkedMapOf<CfirCallableSymbol<*>, CfirPreparedMemberSubstitution>()
        for (member in members) {
            val implicitConstructor = member is CfirConstructor && member.origin == CfirDeclarationOrigin.ImplicitDefault
            val physical = if (family == null || implicitConstructor) owner else checkNotNull(family.physicalOwnerFor(member))
            val logical = if (family == null || implicitConstructor) owner else checkNotNull(family.logicalOwnerFor(member))
            registerSource(physical)
            registerSource(logical)
            checkNotNull(physical as? CfirClassLikeDeclaration).lazyResolveToPhaseWithCallableMembers(CfirResolvePhase.IMPLICIT_TYPES)
            val header = captureInheritanceMemberHeader(member, physical, logical, ::normalize) { parameter ->
                registerSource(parameter.cfir)
                parameter.lazyResolveToPhase(CfirResolvePhase.TYPES)
            } ?: continue
            headers[header.declaration] = header
            val replacements = LinkedHashMap(logicalValues)
            if (family != null) for ((from, to) in family.ownerTypeParameterMappingFor(member)) {
                replacements[from.symbol.toLookupTag()] = signatureSubstitutor.substituteOrSelf(to.symbol.constructType())
            }
            substitutions[header.declaration] = CfirPreparedMemberSubstitution(signatureType, replacements)
        }
        val input = CfirMemberInstantiationInput.Complete(headers, substitutions)
        val slot = declared.signature.slot
        val previous = partition.instantiationHeaders[slot]
        // DeclaredSlot 只记录原域 header；不同 request 的映射属于 ClassScopeInput，不能令声明换代。
        if (previous != null && previous != input.headers) retirePartition(scopeSession, sources, partition)
        partition.instantiationHeaders[slot] = input.headers
        return input
    }

    private fun ownExtendScope(
        scopeSession: ScopeSession,
        sources: ScopeInputSources,
        partition: ScopeCachePartition,
        request: ClassScopeRequest,
        declared: DeclaredScopeInput,
        requiredPhase: CfirResolvePhase?,
    ): CfirPreparedScopeProduct<OwnExtendScopeValue> {
        val epoch = partition.epoch
        if (request.kind != CfirClassMemberScopeKind.USE_SITE || sources.extends == null || request.ownerType == null) {
            return CfirPreparedScopeProduct(OwnExtendScopeValue(null), setOf(epoch))
        }
        val receiver = request.ownerType
        val extendRequest = ExtendScopeInput(
            receiver.expandedExtendTargetKey ?: CfirExtendTargetKey.ClassLike(request.symbol.classId),
            receiver, request.view, request.owner, request.excludedExtend, request.bareStaticExtends, request.symbol.cfir,
        )
        val input = prepareExtendScopeInput(scopeSession, sources, partition, extendRequest, requiredPhase)
        val dependencies = input.dependencies + epoch
        if (!dependencies.all(CfirScopeCacheEpoch::isCurrent)) {
            return CfirPreparedScopeProduct(OwnExtendScopeValue(null), dependencies)
        }
        val result = scopeSession.getOrBuild(
            epoch.owner.id(OwnExtendScopeInput(request, declared, input.value.members)), OWN_EXTEND_SCOPE,
        ) {
            CfirExtendMemberScope(
                scopeSession = scopeSession,
                scopeInput = input.value.members,
                targetKey = extendRequest.target,
                extendProvider = sources.extends,
                session = sources.session,
                receiverType = receiver,
                allowBareGenericStaticQualifierExtends = request.bareStaticExtends,
                excludingExtend = request.excludedExtend,
                scopeOwner = request.owner,
                memberScopeView = request.view,
                instantiationOwner = request.symbol.cfir,
            )
        }
        return CfirPreparedScopeProduct(OwnExtendScopeValue(result), dependencies)
    }

    private fun buildParentScopes(
        sources: ScopeInputSources,
        request: ClassScopeRequest,
        scopeSession: ScopeSession,
        supertypePath: CfirSupertypePath,
        requiredPhase: CfirResolvePhase?,
        dependencies: MutableSet<CfirScopeCacheEpoch>,
    ): List<Pair<CfirInstantiatedSupertypeDescriptor, CfirTypeScope>> {
        val rootType = request.ownerType ?: return emptyList()
        val provenance = request.provenanceType ?: rootType
        if (request.symbol.cfir.origin.fromSource) {
            val ownerSources = inputSources(request.symbol.cfir.moduleData.session)
            val ownerPartition = partition(scopeSession, ownerSources)
            refreshPartition(scopeSession, ownerSources, ownerPartition)
            dependencies += ownerPartition.epoch
        }
        val aliasExpansion = scopeAliasExpansion(sources, scopeSession, dependencies,
            request.kind == CfirClassMemberScopeKind.USE_SITE && sources.directSupertypes != null &&
                sources.directSupertypes !== sources.session.directSupertypeProviderOrNull)
        val descriptors = directParentDescriptorsOf(sources, request, rootType, provenance, aliasExpansion) { type ->
            val symbol = (if (type is ConeLookupTagBasedType) type.lookupTag.toSymbol(sources.session, sources.symbols)
                else type.classIdOrPrimitiveClassId?.let(sources.symbols::getClassLikeSymbolByClassId)) as? CfirClassLikeSymbol<*>
            if (symbol == null) false
            else if (request.kind == CfirClassMemberScopeKind.USE_SITE && sources.directSupertypes != null &&
                sources.directSupertypes !== sources.session.directSupertypeProviderOrNull) {
                checkNotNull(sources.directSupertypes.getPublishedReferenceCycleState(symbol)) {
                    "The selected scope graph has no published cycle input for $symbol"
                }
            } else {
                if (symbol.cfir.origin.fromSource) {
                    val ownerSources = inputSources(symbol.cfir.moduleData.session)
                    val ownerPartition = partition(scopeSession, ownerSources)
                    refreshPartition(scopeSession, ownerSources, ownerPartition)
                    dependencies += ownerPartition.epoch
                }
                type.isReferenceCycleTarget(sources.session, sources.symbols)
            }
        }
        return descriptors.mapNotNull { descriptor ->
            val parentType = descriptor.type
            val classId = parentType.classIdOrPrimitiveClassId ?: return@mapNotNull null
            val symbol = (if (parentType is ConeLookupTagBasedType) parentType.lookupTag.toSymbol(sources.session, sources.symbols)
                else sources.symbols.getClassLikeSymbolByClassId(classId)) as? CfirClassLikeSymbol<*> ?: return@mapNotNull null
            check(!supertypePath.contains(symbol)) {
                "Completed ordinary parent input contains an unmarked nominal reference cycle"
            }
            val lookup = when (descriptor.origin) {
                is CfirInstantiatedSupertypeOrigin.Extend -> CfirCallableLookupProvenance.inheritedThroughExtend(descriptor)
                is CfirInstantiatedSupertypeOrigin.Declared ->
                    request.lookupProvenance
            }
            fun prepareParent(): CfirClassUseSiteMemberScope = sources.session.cangjieScopeProvider.getUseSiteMemberScope(
                classSymbol = symbol,
                useSiteSession = sources.session,
                scopeSession = scopeSession,
                symbolProvider = sources.symbols,
                extendProvider = sources.extends,
                directSupertypeProvider = sources.directSupertypes,
                ownerType = parentType,
                inheritanceProvenanceOwnerType = descriptor.provenanceType,
                dispatchReceiverType = request.dispatchType ?: rootType,
                scopeKind = if (request.kind == CfirClassMemberScopeKind.BODY_LOOKUP) CfirClassMemberScopeKind.USE_SITE
                    else request.kind,
                allowBareGenericStaticQualifierExtends = request.bareStaticExtends,
                excludingExtend = request.excludedExtend,
                inheritedLookupProvenance = lookup,
                supertypePath = supertypePath.child(symbol),
                scopeOwner = request.owner.forInheritedScope(),
                memberScopeView = request.view,
                memberRequiredPhase = requiredPhase,
            )
            val parent = prepareParent()
            dependencies.addAll(parent.cacheDependencies)
            val substituted = if (
                request.dispatchType != null && symbol.isBound && symbol.cfir.typeParameters.isNotEmpty() &&
                (parentType as? ConeLookupTagBasedType)?.typeArguments?.isNotEmpty() == true
            ) {
                val wrapper = getClassSubstitutionScope(sources.session, scopeSession, ::prepareParent, request.dispatchType, parentType)
                dependencies.addAll(wrapper.dependencies)
                wrapper.value
            } else parent
            descriptor to substituted
        }
    }

    /** 替换 wrapper 绑定当前 raw/完整替换类型，并与该节点的域一起退役。 */
    internal fun getClassSubstitutionScope(
        session: CfirSession,
        scopeSession: ScopeSession,
        prepareRaw: () -> CfirClassUseSiteMemberScope,
        dispatchType: ConeCangJieType,
        substitutionOwnerType: ConeCangJieType? = null,
    ): CfirPreparedScopeProduct<CfirClassSubstitutionScope> {
        fun prepareCurrent(): CfirPreparedScopeProduct<CfirClassSubstitutionScope> {
            while (true) {
                val raw = prepareRaw()
                if (raw.cacheGeneration != raw.cacheOwner.generation || !raw.cacheDependencies.all(CfirScopeCacheEpoch::isCurrent)) continue
                val result = scopeSession.getOrBuild(
                    raw.cacheOwner.id(SubstitutionScopeInput(raw, session, dispatchType, substitutionOwnerType)),
                    CLASS_SUBSTITUTION_SCOPE,
                ) {
                    CfirPreparedScopeProduct(
                        CfirClassSubstitutionScope(session, raw, dispatchType, substitutionOwnerType), raw.cacheDependencies,
                    )
                }
                if (result.isCurrent()) return result
            }
        }
        return withScopeConstruction(::prepareCurrent)
    }

    private data class SubstitutionScopeInput(
        val raw: CfirClassUseSiteMemberScope,
        val session: CfirSession,
        val dispatchType: ConeCangJieType,
        val substitutionOwnerType: ConeCangJieType?,
    )

    /** 外围缓存先准备真实输入；查询/constructor 发布新代时重取最终 key。 */
    internal fun <INPUT : Any, PRODUCT : Any> cacheWithPreparedInputs(
        session: CfirSession,
        scopeSession: ScopeSession,
        key: ScopeSessionKey<ScopeSessionOwnedId<CfirPreparedScopeProduct<INPUT>>, CfirPreparedScopeProduct<PRODUCT>>,
        prepare: () -> CfirPreparedScopeProduct<INPUT>,
        create: (INPUT) -> PRODUCT,
    ): CfirPreparedScopeProduct<PRODUCT> {
        fun prepareCurrent(): CfirPreparedScopeProduct<PRODUCT> {
            while (true) {
                val sources = inputSources(session)
                val partition = partition(scopeSession, sources)
                val generation = refreshPartition(scopeSession, sources, partition)
                val input = prepare()
                if (refreshPartition(scopeSession, sources, partition) != generation) continue
                if (!input.isCurrent()) continue
                val dependencies = input.dependencies + partition.epoch
                partition.dependencies.addAll(dependencies.filter { it.owner !== partition.owner })
                val value = scopeSession.getOrBuildOwned(partition.owner.id(input), key) {
                    CfirPreparedScopeProduct(create(input.value), dependencies)
                }
                if (refreshPartition(scopeSession, sources, partition) != generation) continue
                if (value.isCurrent()) return value
            }
        }
        return withScopeConstruction(::prepareCurrent)
    }

    private fun directParentDescriptorsOf(
        sources: ScopeInputSources,
        request: ClassScopeRequest,
        concreteType: ConeCangJieType,
        provenanceType: ConeCangJieType,
        aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?,
        isCycleTarget: (ConeCangJieType) -> Boolean,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        val session = sources.session
        if (concreteType is ConeErrorType) {
            // 错误 owner 没有可供泛型替换的有效实参向量；继续使用声明 owner 保留可诊断的成员面。
            return directParentDescriptorsFor(sources, request, provenanceType, aliasExpansion, isCycleTarget)
        }
        if (provenanceType is ConeErrorType) {
            return directParentDescriptorsFor(sources, request, concreteType, aliasExpansion, isCycleTarget)
        }
        if (concreteType == provenanceType) {
            // 两个视图相同时，父边的声明级身份仍须保留当前 owner 已代入的实参。
            // provider 的 provenanceType 属于父边原始声明空间，直接传播会在多层继承中
            // 把外层 owner 的类型参数重置为中间父类的类型参数。
            return directParentDescriptorsFor(sources, request, concreteType, aliasExpansion, isCycleTarget).map { descriptor ->
                descriptor.copy(provenanceType = descriptor.type)
            }
        }

        val normalizedProvenanceType = provenanceType.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
        val normalizedConcreteType = concreteType.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
        val provenanceToConcreteSubstitutor =
            normalizedProvenanceType.provenanceToConcreteSubstitutor(normalizedConcreteType, session.typeContext)
        val provenanceDescriptors = directParentDescriptorsFor(sources, request, provenanceType, aliasExpansion, isCycleTarget).map { descriptor ->
            descriptor.copy(
                type = provenanceToConcreteSubstitutor
                    .substituteOrSelf(descriptor.type)
                    .fullyExpandedType(session, sources.symbols, aliasExpansion),
                provenanceType = descriptor.type.fullyExpandedType(session, sources.symbols, aliasExpansion),
            )
        }
        val representedOrigins = provenanceDescriptors.mapTo(linkedSetOf()) { it.origin }

        return buildList {
            addAll(provenanceDescriptors)
            for (descriptor in directParentDescriptorsFor(sources, request, concreteType, aliasExpansion, isCycleTarget)) {
                if (descriptor.origin in representedOrigins) continue
                // 仅具体类型适用的 extend 父边不属于泛型声明形态，但仍是当前成员图的独立输入。
                add(descriptor)
            }
        }
    }

    private fun directParentDescriptorsFor(sources: ScopeInputSources, request: ClassScopeRequest, type: ConeCangJieType,
        aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?,
        isCycleTarget: (ConeCangJieType) -> Boolean): List<CfirInstantiatedSupertypeDescriptor> {
        val session = sources.session
        val classSymbol = request.symbol
        val scopeKind = request.kind
        val scopeOwner = request.owner
        val excludingExtend = request.excludedExtend
        val symbolProvider = sources.symbols
        val directSupertypeProvider = sources.directSupertypes
        val descriptors = if (
            scopeKind == CfirClassMemberScopeKind.DECLARATION_SITE ||
            scopeKind == CfirClassMemberScopeKind.BODY_LOOKUP
        ) {
            val substitutor = createMemberDeclarationSubstitutor(classSymbol, type, session)
            declaredParentRefs(classSymbol).mapNotNull { superTypeRef ->
                val supertype = superTypeRef.ordinaryMemberSupertypeOrNull(
                    classSymbol.cfir, session, { it.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion) }, sources.symbols, isCycleTarget,
                ) ?: return@mapNotNull null
                CfirInstantiatedSupertypeDescriptor(
                    type = substitutor?.substituteOrSelf(supertype) ?: supertype,
                    origin = CfirInstantiatedSupertypeOrigin.Declared(superTypeRef),
                )
            }
        } else {
            val typeAwareSupertypeProvider = sources.typeAwareSupertypes
            when {
                typeAwareSupertypeProvider != null -> {
                    // type-only 查询必须携带本次 actual root；该局部 lookup 视图不写回
                    // request 的原 self/provenance，更不成为成员存储的 structType。
                    val lookupType = type.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        .forParentLookupRoot(classSymbol)
                    typeAwareSupertypeProvider.getDirectSupertypeDescriptors(lookupType).filter { descriptor ->
                        excludingExtend == null || (descriptor.origin as? CfirInstantiatedSupertypeOrigin.Extend)?.sourceExtend !== excludingExtend
                    }
                }
                directSupertypeProvider != null -> {
                    directParentDescriptorsFromEdges(sources, type, classSymbol, excludedExtend = null, aliasExpansion, isCycleTarget)
                }
                else -> {
                    val substitutor = createMemberDeclarationSubstitutor(classSymbol, type, session)
                    declaredParentRefs(classSymbol).mapNotNull { superTypeRef ->
                        val supertype = superTypeRef.ordinaryMemberSupertypeOrNull(
                            classSymbol.cfir, session, { it.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion) }, sources.symbols, isCycleTarget,
                        ) ?: return@mapNotNull null
                        CfirInstantiatedSupertypeDescriptor(
                            type = substitutor?.substituteOrSelf(supertype) ?: supertype,
                            origin = CfirInstantiatedSupertypeOrigin.Declared(superTypeRef),
                        )
                    }
                }
            }
        }

        val ownerScopedDescriptors = descriptors.filter { descriptor ->
            scopeOwner.includesDirectSupertype(descriptor.origin)
        }

        return ownerScopedDescriptors.map { descriptor ->
            descriptor.copy(
                type = descriptor.type.fullyExpandedType(session, sources.symbols, aliasExpansion),
                provenanceType = descriptor.provenanceType.fullyExpandedType(session, sources.symbols, aliasExpansion),
            )
        }
    }

    /** 仅用于父图查询的实际名义 root 视图，保留实参、属性与全部类型标志。 */
    private fun ConeCangJieType.forParentLookupRoot(symbol: CfirClassLikeSymbol<*>): ConeCangJieType {
        if (this !is ConeClassifierType) return this // builtin 查询走其独立类型域。
        val originalTag = lookupTag
        check(originalTag.classId == symbol.classId) { "Parent query type does not describe its actual root" }
        if (originalTag is ConeClassLikeLookupTagWithFixedSymbol) {
            check(originalTag.symbol === symbol) { "Parent query cannot replace a fixed declaration root" }
            return this
        }
        val tag = ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol)
        return when (this) {
            is ConeClassLikeType -> ConeClassLikeType(tag, typeArguments, attributes, isInterface, isThisType, rawArrayDimensions)
            is ConeStructType -> ConeStructType(tag, typeArguments, attributes)
            is ConeEnumType -> ConeEnumType(tag, typeArguments, attributes, isRefEnum)
            else -> error("Parent query requires a completed nominal root type")
        }
    }

    /** 每次 alias 读取固定到实际来源，并把其 owner 发布 epoch 放入同一 scope 产品。 */
    private fun scopeAliasExpansion(
        sources: ScopeInputSources,
        scopeSession: ScopeSession,
        dependencies: MutableSet<CfirScopeCacheEpoch>,
        selectedGraph: Boolean,
    ): (CfirTypeAlias) -> ConeCangJieType? = { alias ->
        if (selectedGraph) {
            val product = checkNotNull(checkNotNull(sources.directSupertypes).getCompletedSupertypeProduct(alias.symbol)) {
                "The selected scope graph has no completed alias RHS: ${alias.symbol}"
            }
            checkNotNull(product.superTypeRefs.singleOrNull()?.coneTypeOrNull)
        } else {
            if (alias.origin.fromSource) {
                val ownerSources = inputSources(alias.moduleData.session)
                val ownerPartition = partition(scopeSession, ownerSources)
                refreshPartition(scopeSession, ownerSources, ownerPartition)
                dependencies += ownerPartition.epoch
            }
            alias.expandedConeTypeWithEnsuredPhase()
        }
    }

    /** 普通 Source 声明也读取自己的完成引用；library header 由其原加载事务拥有。 */
    private fun declaredParentRefs(symbol: CfirClassLikeSymbol<*>): List<CfirTypeRef> {
        val declaration = symbol.cfir
        if (!declaration.origin.fromSource) return declaration.superTypeRefs
        return checkNotNull(checkNotNull(declaration.moduleData.session.directSupertypeProviderOrNull)
            .getCompletedSupertypeProduct(symbol)) { "The original Source scope has no completed supertype product: $symbol" }
            .superTypeRefs
    }

    /**
     * 在 extend 声明体内查询父类型时按 graph edge 来源跳过当前 extend 自己贡献的接口。
     */
    private fun directParentDescriptorsFromEdges(
        sources: ScopeInputSources,
        type: ConeCangJieType,
        ownerSymbol: CfirClassLikeSymbol<*>,
        excludedExtend: CfirExtend?,
        aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?,
        isCycleTarget: (ConeCangJieType) -> Boolean,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        val symbolProvider = sources.symbols
        val directSupertypeProvider = sources.directSupertypes
        val ownerDeclaration = ownerSymbol.cfir
        val declarationSubstitutor = createMemberDeclarationSubstitutor(ownerSymbol, type, sources.session)
        val product = checkNotNull(checkNotNull(directSupertypeProvider).getCompletedSupertypeProduct(ownerSymbol)) {
            "The selected supertype source has no completed product for $ownerSymbol"
        }
        val directSupertypes = product.superTypes
            .mapNotNull { edge ->
                if (excludedExtend != null && edge.sourceExtend === excludedExtend) return@mapNotNull null
                edge.toUseSiteDescriptor(sources.session, type, ownerDeclaration, declarationSubstitutor, sources.symbols, aliasExpansion, isCycleTarget)
            }
        return directSupertypes
    }

    /**
     * 将 supertype graph edge 还原为当前 owner type 上的来源保留父边。
     */
    private fun CfirSuperTypeGraphEdge.toUseSiteDescriptor(
        session: CfirSession,
        ownerType: ConeCangJieType,
        ownerDeclaration: CfirClassLikeDeclaration?,
        declarationSubstitutor: ConeSubstitutor?,
        symbols: CfirSymbolProvider,
        aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?,
        isCycleTarget: (ConeCangJieType) -> Boolean,
    ): CfirInstantiatedSupertypeDescriptor? {
        val extend = sourceExtend
        val supertype = if (extend == null) {
            typeRef.ordinaryMemberSupertypeOrNull(
                ownerDeclaration ?: return null, session, { it.fullyExpandedTypeUsingAbbreviation(session, symbols, aliasExpansion) }, symbols, isCycleTarget,
            ) ?: return null
        } else {
            val type = typeRef.classifyDeclaredSupertype { it.fullyExpandedTypeUsingAbbreviation(session, symbols, aliasExpansion) }.scopeTraversalTypeOrNull() ?: return null
            if (isCycleTarget(type)) return null
            type
        }
        if (extend != null) {
            if (!supertype.isInterface) return null
            val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: return null
            val substitution = createExtendDeclarationSubstitution(
                session = session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = ownerType,
                symbolProvider = symbols,
                expandedConeType = aliasExpansion,
            ) ?: return null
            return CfirInstantiatedSupertypeDescriptor(
                type = substitution.substitutor.substituteOrSelf(supertype),
                origin = CfirInstantiatedSupertypeOrigin.Extend(
                    sourceExtend = extend,
                    declarationPackage = requireNotNull(extend.getDeclarationPackage()) {
                        "Extend declaration package is not indexed: ${extend.symbol}"
                    },
                    sourceTypeRef = typeRef,
                ),
            )
        }
        return CfirInstantiatedSupertypeDescriptor(
            type = declarationSubstitutor?.substituteOrSelf(supertype) ?: supertype,
            origin = CfirInstantiatedSupertypeOrigin.Declared(typeRef),
        )
    }

    private data class ExtendScopeInput(
        val target: CfirExtendTargetKey,
        val receiver: ConeCangJieType,
        val view: CfirCjmpMemberScopeView,
        val owner: CfirMemberScopeOwner,
        val excluded: CfirExtend?,
        val bare: Boolean,
        val instantiationOwner: CfirDeclaration?,
    )

    private data class PreparedExtendScopeInput(
        val request: ExtendScopeInput,
        val members: CfirExtendMemberScope.ScopeInput,
    )

    /** 空值仅表示此查询没有直接 extend；依赖证明失效时调用方必须重新准备。 */
    private data class OwnExtendScopeValue(val scope: CfirExtendMemberScope?)

    private data class OwnExtendScopeInput(
        val request: ClassScopeRequest,
        val declared: DeclaredScopeInput,
        val members: CfirExtendMemberScope.ScopeInput,
    )

    private fun prepareExtendScopeInput(
        scopeSession: ScopeSession,
        sources: ScopeInputSources,
        partition: ScopeCachePartition,
        request: ExtendScopeInput,
        requiredPhase: CfirResolvePhase?,
    ): CfirPreparedScopeProduct<PreparedExtendScopeInput> {
        refreshPartition(scopeSession, sources, partition)
        val epoch = partition.epoch
        val dependencies = linkedSetOf(epoch)
        val aliasExpansion = scopeAliasExpansion(sources, scopeSession, dependencies,
            sources.directSupertypes != null && sources.directSupertypes !== sources.session.directSupertypeProviderOrNull)
        val members = CfirExtendMemberScope.prepareScopeInput(
            request.target, checkNotNull(sources.extends), sources.session, request.receiver, request.view, requiredPhase,
            request.owner, request.instantiationOwner,
            CfirTypeAwareSupertypeSources(sources.session, sources.symbols, sources.extends, sources.directSupertypes),
            sources.typeAwareSupertypes,
            request.bare, aliasExpansion,
            registerParameterOwner = { parameter ->
                parameter.lazyResolveToPhase(CfirResolvePhase.TYPES)
                if (parameter.cfir.origin.fromSource) {
                    val ownerSources = inputSources(parameter.cfir.moduleData.session)
                    val ownerPartition = partition(scopeSession, ownerSources)
                    refreshPartition(scopeSession, ownerSources, ownerPartition)
                    dependencies += ownerPartition.epoch
                }
            },
        )
        val completedMembers = if (request.view == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) {
            fun prepareParent(type: ConeCangJieType, owner: CfirMemberScopeOwner): CfirTypeScope? {
                val semanticType = type.fullyExpandedTypeUsingAbbreviation(sources.session, sources.symbols, aliasExpansion)
                val provenance = semanticType.classIdOrPrimitiveClassId?.let(sources.symbols::getClassLikeSymbolByClassId)
                    ?.let(::declarationSelfType) ?: semanticType
                val product = prepareMemberScopeForUseSiteType(
                    sources.session, sources, scopeSession,
                    NonNominalScopeRequest(
                        semanticType, provenance, semanticType, null, owner, CfirCallableLookupProvenance.None,
                        request.view, CfirClassMemberScopeKind.USE_SITE, CfirClassMemberScopeKind.USE_SITE,
                    ), emptySet(), CfirResolvePhase.CJMP_MATCHING,
                ) ?: return null
                dependencies.addAll(product.dependencies)
                return product.value
            }
            // 索引桶包含目标同名但约束不成立的 extend；只为真实适用 owner 准备发布父表。
            val publishedOwners = members.owners.keys.filter(request.owner::includesDirectExtend).mapNotNull { owner ->
                members.receivers.getValue(CfirExtendMemberScope.ReceiverKey(owner, request.receiver)).ownerSubstitution?.let { value ->
                    owner to createTypeSubstitutorByTypeConstructor(value.replacements, sources.session.typeContext, false)
                }
            }.toMap()
            val nominal = request.receiver.takeIf { publishedOwners.isNotEmpty() }?.classIdOrPrimitiveClassId
                ?.let(sources.symbols::getClassLikeSymbolByClassId)
                ?.let { prepareParent(request.receiver, CfirMemberScopeOwner.NominalInstantiation) }
            val interfaces = publishedOwners.mapValues { (owner, substitutor) ->
                val input = members.owners.getValue(owner)
                val seenInterfaces = linkedSetOf<ConeCangJieType>()
                input.superTypeRefs.mapNotNull { ref ->
                    val type = ref.coneTypeOrNull ?: return@mapNotNull null
                    val concrete = substitutor.substituteOrSelf(type)
                        .fullyExpandedTypeUsingAbbreviation(sources.session, sources.symbols, aliasExpansion)
                    if (!seenInterfaces.add(concrete)) return@mapNotNull null
                    val parent = prepareParent(concrete, CfirMemberScopeOwner.DeclarationOnlyInstantiation)
                        ?: return@mapNotNull null
                    CfirExtendMemberScope.InheritedInterfaceInput(ref, concrete, parent,
                        org.cangnova.cangjie.cfir.scopes.impl.captureInheritanceScopeMembers(parent, concrete, null, true,
                            members.sources, aliasExpansion))
                }
            }
            val peerInterfaces = members.peerOwners.mapValues { (pair, input) ->
                input.interfaces.mapNotNull { (ref, concrete) ->
                    val parent = prepareParent(concrete, CfirMemberScopeOwner.DeclarationOnlyInstantiation) ?: return@mapNotNull null
                    CfirExtendMemberScope.InheritedInterfaceInput(ref, concrete, parent,
                        org.cangnova.cangjie.cfir.scopes.impl.captureInheritanceScopeMembers(parent, concrete, pair.second, true,
                            members.sources, aliasExpansion))
                }
            }
            members.copy(inheritedInterfaces = java.util.Collections.unmodifiableMap(interfaces.mapValues { (_, values) ->
                java.util.Collections.unmodifiableList(ArrayList(values))
            }), nominalScope = nominal, peerInheritedInterfaces = java.util.Collections.unmodifiableMap(peerInterfaces.mapValues { (_, values) ->
                java.util.Collections.unmodifiableList(ArrayList(values))
            }),
                nominalInheritanceMembers = nominal?.let {
                    org.cangnova.cangjie.cfir.scopes.impl.captureInheritanceScopeMembers(it, request.receiver, null, false,
                        members.sources, aliasExpansion)
                }.orEmpty())
        } else members
        val prepared = CfirPreparedScopeProduct(PreparedExtendScopeInput(request, completedMembers), dependencies)
        if (refreshPartition(scopeSession, sources, partition) != epoch.generation || !prepared.isCurrent()) return prepared
        val previous = partition.extensions[request]
        if (previous != null && previous != completedMembers) retirePartition(scopeSession, sources, partition)
        partition.extensions[request] = completedMembers
        return prepared
    }

    /** 非 nominal 和 primitive qualifier 也由同一域完成来源阶段与 scope 共享。 */
    fun getDeclarationInheritanceInput(owner: CfirExtend, session: CfirSession, scopeSession: ScopeSession): CfirDeclarationInheritanceInput? {
        owner.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val sources = inputSources(session)
        val inputPartition = partition(scopeSession, sources)
        while (true) {
            refreshPartition(scopeSession, sources, inputPartition)
            val epoch = inputPartition.epoch
            val dependencies = linkedSetOf(epoch)
            val aliasExpansion = scopeAliasExpansion(sources, scopeSession, dependencies, false)
            // 原请求的 alias 读取进入拥有域；失效后从 owner 原引用重新准备。
            val type = owner.extendedTypeRef.coneTypeOrNull
                ?.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion) ?: return null
            if (type.containsErrorType()) return null
            val key = type.extendTargetKey ?: return null
            val peers = session.extendProvider.getExtendsForTarget(key).toSet() + owner
            val scope = getExtendMemberScope(session, scopeSession, key, type, CfirCjmpMemberScopeView.CJMP_INSTANTIATION,
                CfirResolvePhase.TYPES, owner = CfirMemberScopeOwner.ExtendInstantiation(owner, peers), instantiationOwner = owner)
            dependencies.addAll(scope.dependencies)
            val input = CfirPreparedScopeProduct(scope.value.declarationInheritanceInput(owner), dependencies)
            if (refreshPartition(scopeSession, sources, inputPartition) != epoch.generation || !input.isCurrent()) continue
            return input.value
        }
    }

    /** 非 nominal 和 primitive qualifier 也由同一域完成来源阶段与 scope 共享。 */
    internal fun getExtendMemberScope(
        session: CfirSession,
        scopeSession: ScopeSession,
        target: CfirExtendTargetKey,
        receiver: ConeCangJieType,
        view: CfirCjmpMemberScopeView,
        requiredPhase: CfirResolvePhase?,
        owner: CfirMemberScopeOwner = CfirMemberScopeOwner.UseSite,
        excluded: CfirExtend? = null,
        bare: Boolean = false,
        instantiationOwner: CfirDeclaration? = owner.extendOwner,
        symbolProvider: CfirSymbolProvider = session.symbolProvider,
        extendProvider: CfirExtendProvider = session.extendProvider,
        directSupertypeProvider: CfirDirectSupertypeProvider? = session.directSupertypeProviderOrNull,
    ): CfirPreparedScopeProduct<CfirExtendMemberScope> = cacheWithPreparedInputs(
        session, scopeSession, EXTEND_MEMBER_SCOPE,
        prepare = {
            val sources = inputSources(session, symbolProvider, extendProvider, directSupertypeProvider)
            prepareExtendScopeInput(
                scopeSession, sources, partition(scopeSession, sources),
                ExtendScopeInput(target, receiver, view, owner, excluded, bare, instantiationOwner), requiredPhase,
            )
        },
        create = { input ->
            CfirExtendMemberScope(
                session = session,
                scopeSession = scopeSession,
                scopeInput = input.members,
                targetKey = input.request.target,
                receiverType = input.request.receiver,
                extendProvider = checkNotNull(input.members.sources.extends),
                memberScopeView = input.request.view,
                scopeOwner = input.request.owner,
                excludingExtend = input.request.excluded,
                allowBareGenericStaticQualifierExtends = input.request.bare,
                instantiationOwner = input.request.instantiationOwner,
            )
        },
    )

    private data class GenericSubstitutionScopeInput(
        val scope: CfirTypeScope,
        val dispatchType: ConeCangJieType,
        val substitutionOwnerType: ConeCangJieType?,
    )

    private data class NonNominalScopeRequest(
        val ownerType: ConeCangJieType,
        val provenanceType: ConeCangJieType,
        val dispatchType: ConeCangJieType,
        val excludedExtend: CfirExtend?,
        val owner: CfirMemberScopeOwner,
        val lookupProvenance: CfirCallableLookupProvenance,
        val view: CfirCjmpMemberScopeView,
        val kind: CfirClassMemberScopeKind,
        val parentKind: CfirClassMemberScopeKind,
        val deriveDeclarationProvenance: Boolean = false,
    )

    private data class NonNominalScopeInput(
        val request: NonNominalScopeRequest,
        val descriptors: List<CfirInstantiatedSupertypeDescriptor>,
        val scopes: List<CfirTypeScope>,
    )

    /** 任意 receiver 的结构成员图由 provider 统一准备，raw 类型工厂只转发正式请求。 */
    internal fun getMemberScopeForUseSiteType(
        session: CfirSession,
        ownerType: ConeCangJieType,
        scopeSession: ScopeSession,
        excludingExtend: CfirExtend?,
        scopeOwner: CfirMemberScopeOwner,
        memberScopeView: CfirCjmpMemberScopeView,
        memberRequiredPhase: CfirResolvePhase?,
        scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
    ): CfirTypeScope? = withScopeConstruction {
        val sources = inputSources(session)
        prepareMemberScopeForUseSiteType(
            session, sources, scopeSession,
            NonNominalScopeRequest(ownerType, ownerType, ownerType, excludingExtend, scopeOwner, CfirCallableLookupProvenance.None,
                memberScopeView, scopeKind, scopeKind, deriveDeclarationProvenance = true),
            emptySet(), memberRequiredPhase,
        )?.value
    }

    /**
     * 声明继承检查的类型 scope。名义声明只读取自身声明父图；没有名义声明的 builtin
     * 由直接 extend 声明及其接口组成，接口仍按声明视图读取，不能在 checker 中另造父树。
     */
    fun getDeclarationSiteMemberScopeForType(
        session: CfirSession,
        scopeSession: ScopeSession,
        type: ConeCangJieType,
        excludingExtend: CfirExtend?,
        memberRequiredPhase: CfirResolvePhase?,
    ): CfirTypeScope? = withScopeConstruction {
        val sources = inputSources(session)
        prepareMemberScopeForUseSiteType(
            session, sources, scopeSession,
            NonNominalScopeRequest(
                type, type, type, excludingExtend, CfirMemberScopeOwner.UseSite,
                CfirCallableLookupProvenance.None, CfirCjmpMemberScopeView.RAW, CfirClassMemberScopeKind.DECLARATION_SITE,
                CfirClassMemberScopeKind.DECLARATION_SITE, deriveDeclarationProvenance = true,
            ),
            emptySet(), memberRequiredPhase,
        )?.value
    }

    private fun prepareMemberScopeForUseSiteType(
        session: CfirSession,
        sources: ScopeInputSources,
        scopeSession: ScopeSession,
        originalRequest: NonNominalScopeRequest,
        typePath: Set<ConeCangJieType>,
        requiredPhase: CfirResolvePhase?,
    ): CfirPreparedScopeProduct<CfirTypeScope>? {
        while (true) {
            val product = prepareMemberScopeForUseSiteTypeOnce(session, sources, scopeSession, originalRequest, typePath, requiredPhase)
                ?: return null
            if (product.isCurrent()) return product
        }
    }

    /** 失效后丢弃整个派生请求，不在旧 alias epoch 的内部缓存循环上重试。 */
    private fun prepareMemberScopeForUseSiteTypeOnce(
        session: CfirSession,
        sources: ScopeInputSources,
        scopeSession: ScopeSession,
        originalRequest: NonNominalScopeRequest,
        typePath: Set<ConeCangJieType>,
        requiredPhase: CfirResolvePhase?,
    ): CfirPreparedScopeProduct<CfirTypeScope>? {
        // 分类器与非名义目标键都只读取本次来源完成的语义类型，不读取 alias 节点旧 metadata。
        val aliasDependencies = linkedSetOf<CfirScopeCacheEpoch>()
        val aliasExpansion = scopeAliasExpansion(sources, scopeSession, aliasDependencies,
            originalRequest.kind == CfirClassMemberScopeKind.USE_SITE && sources.directSupertypes != null &&
                sources.directSupertypes !== session.directSupertypeProviderOrNull)
        val owner = originalRequest.ownerType.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
        val provenance = if (originalRequest.deriveDeclarationProvenance) {
            (if (owner is ConeLookupTagBasedType) owner.lookupTag.toSymbol(session, sources.symbols)
                else owner.classIdOrPrimitiveClassId?.let(sources.symbols::getClassLikeSymbolByClassId))
                ?.let { declarationSelfType(it as CfirClassLikeSymbol<*>) } ?: owner
        } else originalRequest.provenanceType.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
        val dispatch = originalRequest.dispatchType.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
        if (owner is ConeTypeAliasType || provenance is ConeTypeAliasType || dispatch is ConeTypeAliasType) return null
        val kind = if (originalRequest.kind == CfirClassMemberScopeKind.DECLARATION_SITE && owner.classIdOrPrimitiveClassId == null)
            CfirClassMemberScopeKind.USE_SITE else originalRequest.kind
        val request = originalRequest.copy(ownerType = owner, provenanceType = provenance, dispatchType = dispatch, kind = kind,
            deriveDeclarationProvenance = false)
        check(request.ownerType !in typePath) { "Completed structural member input contains an unmarked type cycle" }
        val nextTypePath = typePath + request.ownerType
        val classId = request.ownerType.classIdOrPrimitiveClassId
        if (classId != null) {
            val symbol = (if (request.ownerType is ConeLookupTagBasedType) request.ownerType.lookupTag.toSymbol(session, sources.symbols)
                else sources.symbols.getClassLikeSymbolByClassId(classId)) as? CfirClassLikeSymbol<*> ?: return null
            val product = getClassSubstitutionScope(
                session, scopeSession,
                prepareRaw = {
                    getUseSiteMemberScope(
                        classSymbol = symbol,
                        useSiteSession = session,
                        scopeSession = scopeSession,
                        symbolProvider = sources.symbols,
                        extendProvider = if (request.owner.includesExtendScopes) sources.extends else null,
                        directSupertypeProvider = sources.directSupertypes,
                        ownerType = request.ownerType,
                        inheritanceProvenanceOwnerType = request.provenanceType,
                        dispatchReceiverType = request.dispatchType,
                        scopeKind = request.kind,
                        allowBareGenericStaticQualifierExtends = false,
                        excludingExtend = request.excludedExtend,
                        scopeOwner = request.owner,
                        inheritedLookupProvenance = request.lookupProvenance,
                        supertypePath = CfirSupertypePath.root(symbol),
                        memberScopeView = request.view,
                        memberRequiredPhase = requiredPhase,
                    )
                },
                dispatchType = request.dispatchType,
                substitutionOwnerType = request.ownerType,
            )
            return CfirPreparedScopeProduct(product.value, product.dependencies + aliasDependencies)
        }

        val target = request.ownerType.extendTargetKey ?: return null
        // 一次准备只对应一次原请求的 alias 快照；失效产品交回外层重新准备原请求。
        val preparedInput = run {
            val sourcePartition = partition(scopeSession, sources)
            refreshPartition(scopeSession, sources, sourcePartition)
            val sourceEpoch = sourcePartition.epoch
            val direct = if (request.kind == CfirClassMemberScopeKind.USE_SITE && request.owner.includesExtendScopes && sources.extends != null) {
                listOf(getSubstitutionScope(
                    session, scopeSession,
                    prepareScope = {
                        getExtendMemberScope(
                            session, scopeSession, target, request.ownerType, request.view, requiredPhase,
                            owner = request.owner, excluded = request.excludedExtend,
                            symbolProvider = sources.symbols, extendProvider = sources.extends,
                            directSupertypeProvider = sources.directSupertypes,
                        )
                    },
                    dispatchType = request.dispatchType,
                    substitutionOwnerType = request.ownerType,
                ))
            } else emptyList()
            val descriptors = if (request.kind == CfirClassMemberScopeKind.DECLARATION_SITE ||
                request.view == CfirCjmpMemberScopeView.CJMP_INSTANTIATION && request.owner is CfirMemberScopeOwner.ExtendInstantiation
            ) {
                emptyList()
            } else {
                sources.typeAwareSupertypes?.getDirectSupertypeDescriptors(request.ownerType).orEmpty().filter { descriptor ->
                    request.owner.includesDirectSupertype(descriptor.origin) &&
                        (request.excludedExtend == null || (descriptor.origin as? CfirInstantiatedSupertypeOrigin.Extend)?.sourceExtend !== request.excludedExtend)
                }
            }
            val parents = descriptors.mapNotNull { descriptor ->
                val lookup = when (descriptor.origin) {
                    is CfirInstantiatedSupertypeOrigin.Extend -> CfirCallableLookupProvenance.inheritedThroughExtend(descriptor)
                    else -> request.lookupProvenance
                }
                prepareMemberScopeForUseSiteType(
                    session, sources, scopeSession,
                    request.copy(
                        ownerType = descriptor.type,
                        provenanceType = descriptor.provenanceType,
                        owner = request.owner.forInheritedScope(),
                        lookupProvenance = lookup,
                        kind = request.parentKind,
                    ),
                    nextTypePath, requiredPhase,
                )
            }
            val products = direct + parents
            CfirPreparedScopeProduct(
                NonNominalScopeInput(request, descriptors, products.map { it.value }),
                products.flatMapTo(linkedSetOf(sourceEpoch).apply { addAll(aliasDependencies) }) { it.dependencies },
            )
        }
        val value = when (preparedInput.value.scopes.size) {
            0 -> CfirTypeScope.Empty
            1 -> preparedInput.value.scopes.single()
            else -> CfirCompositeTypeScope(preparedInput.value.scopes, session)
        }
        val completed = CfirPreparedScopeProduct(value, preparedInput.dependencies)
        if (!completed.isCurrent()) return completed
        val sourcePartition = partition(scopeSession, sources)
        refreshPartition(scopeSession, sources, sourcePartition)
        if (!completed.isCurrent()) return completed
        return scopeSession.getOrBuildOwned(sourcePartition.owner.id(preparedInput), NON_NOMINAL_MEMBER_SCOPE) {
            completed
        }
    }

    /** 非 nominal 结构作用域的替换外壳也依赖同代完整的基作用域。 */
    internal fun getSubstitutionScope(
        session: CfirSession,
        scopeSession: ScopeSession,
        prepareScope: () -> CfirPreparedScopeProduct<CfirTypeScope>,
        dispatchType: ConeCangJieType,
        substitutionOwnerType: ConeCangJieType?,
    ): CfirPreparedScopeProduct<CfirClassSubstitutionScope> = cacheWithPreparedInputs(
        session, scopeSession, GENERIC_SUBSTITUTION_SCOPE,
        prepare = {
            val base = prepareScope()
            CfirPreparedScopeProduct(GenericSubstitutionScopeInput(base.value, dispatchType, substitutionOwnerType), base.dependencies)
        },
        create = { input -> CfirClassSubstitutionScope(session, input.scope, input.dispatchType, input.substitutionOwnerType) },
    )

    /** 同一遍历跨实际 producer 共享 memo；语义 owner 仍由各物理存储中的分区隔离。 */
    private companion object {
        val construction = ThreadLocal<ScopeConstruction>()
        val SCOPE_CACHE_PARTITION = scopeSessionKey<ScopePartitionKey, ScopeCachePartition>()
        val DECLARED_SCOPE_INPUT = scopeSessionKey<ScopeSessionOwnedId<DeclaredInputSignature>, DeclaredScopeInput>()
        val CLASS_USE_SITE_SCOPE = scopeSessionKey<ScopeSessionOwnedId<ClassScopeInput>, CfirClassUseSiteMemberScope>()
        val OWN_EXTEND_SCOPE = scopeSessionKey<ScopeSessionOwnedId<OwnExtendScopeInput>, CfirExtendMemberScope>()
        val CLASS_SUBSTITUTION_SCOPE = scopeSessionKey<ScopeSessionOwnedId<SubstitutionScopeInput>, CfirPreparedScopeProduct<CfirClassSubstitutionScope>>()
        val USE_SITE_REQUIRED_MEMBERS_PHASE = requiredMembersPhaseStampKey<ScopeSessionOwnedId<ClassScopeRequest>>()
        val EXTEND_MEMBER_SCOPE = scopeSessionKey<ScopeSessionOwnedId<CfirPreparedScopeProduct<PreparedExtendScopeInput>>, CfirPreparedScopeProduct<CfirExtendMemberScope>>()
        val GENERIC_SUBSTITUTION_SCOPE =
            scopeSessionKey<ScopeSessionOwnedId<CfirPreparedScopeProduct<GenericSubstitutionScopeInput>>, CfirPreparedScopeProduct<CfirClassSubstitutionScope>>()
        val NON_NOMINAL_MEMBER_SCOPE = scopeSessionKey<ScopeSessionOwnedId<CfirPreparedScopeProduct<NonNominalScopeInput>>, CfirPreparedScopeProduct<CfirTypeScope>>()
    }

    /**
     * 返回 class 在 use-site 场景下的成员 scope。
     */
    override fun getUseSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
        memberRequiredPhase: CfirResolvePhase?,
    ): CfirTypeScope = klass.useSiteMemberScopeInView(
        useSiteSession,
        scopeSession,
        useSiteMemberScopeView(useSiteSession, memberRequiredPhase),
        memberRequiredPhase,
    )

    /**
     * 返回 typealias 构造器替换 scope。
     */
    override fun getTypealiasConstructorScope(
        typeAlias: CfirTypeAlias,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirScope {
        while (true) {
            typeAlias.lazyResolveToPhase(CfirResolvePhase.TYPES)
            val view = if (useSiteSession.isCjmpSpecificCompilationEnabled()) CfirCjmpMemberScopeView.CJMP_ACTUALIZED
                else CfirCjmpMemberScopeView.RAW
            val sources = inputSources(useSiteSession)
            val aliasDependencies = linkedSetOf<CfirScopeCacheEpoch>()
            val aliasExpansion = scopeAliasExpansion(sources, scopeSession, aliasDependencies, false)
            // RHS 本身也属于本次读取依赖，不从活动 alias 字段推断其发布版本。
            aliasExpansion(typeAlias)
            val sourceRef = if (typeAlias.origin.fromSource) checkNotNull(checkNotNull(typeAlias.moduleData.session.directSupertypeProviderOrNull)
                .getCompletedSupertypeProduct(typeAlias.symbol)).superTypeRefs.single() else typeAlias.expandedTypeRef
            val expanded = sourceRef.coneTypeOrNull?.fullyExpandedType(useSiteSession, sources.symbols, aliasExpansion) as? ConeLookupTagBasedType
                ?: return CfirTypeScope.Empty
            val expandedSymbol = expanded.lookupTag.toSymbol(useSiteSession, sources.symbols) as? CfirClassLikeSymbol<*>
                ?: return CfirTypeScope.Empty
            val partition = partition(scopeSession, sources)
            refreshPartition(scopeSession, sources, partition)
            val aliasInput = sourceRef to expanded
            val aliasSlot = typeAlias.symbol to view
            val previous = partition.aliases[aliasSlot]
            if (previous != null && previous != aliasInput) retirePartition(scopeSession, sources, partition)
            partition.aliases[aliasSlot] = aliasInput
            val generation = partition.owner.generation
            fun prepareRaw(): CfirClassUseSiteMemberScope = getUseSiteMemberScope(
                classSymbol = expandedSymbol,
                useSiteSession = useSiteSession,
                scopeSession = scopeSession,
                ownerType = expanded,
                dispatchReceiverType = expanded,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
                memberScopeView = view,
                memberRequiredPhase = if (view == CfirCjmpMemberScopeView.CJMP_ACTUALIZED) CfirResolvePhase.CJMP_MATCHING
                    else CfirResolvePhase.STATUS,
            )
            val parent = getClassSubstitutionScope(useSiteSession, scopeSession, ::prepareRaw, expanded)
            // RHS 与父图都可能在正规准备中替换，不能以旧 RHS 包装刚生成的新父节点。
            if (!parent.isCurrent() || aliasDependencies.any { !it.isCurrent() }) continue
            if (refreshPartition(scopeSession, sources, partition) != generation) continue
            val dependencies = parent.dependencies + partition.epoch + aliasDependencies
            partition.dependencies.addAll(dependencies.filter { it.owner !== partition.owner })
            val key = TypeAliasConstructorScopeKey(typeAlias.symbol, sourceRef, expanded, view, parent.value)
            val result = scopeSession.getOrBuild(partition.owner.id(key), TYPEALIAS_CONSTRUCTOR) {
                CfirPreparedScopeProduct<CfirScope>(
                    TypeAliasConstructorsSubstitutingScope.initialize(typeAlias.symbol, useSiteSession, parent.value), dependencies,
                )
            }
            if (aliasDependencies.any { !it.isCurrent() }) continue
            if (refreshPartition(scopeSession, sources, partition) != generation) continue
            if (result.isCurrent()) return result.value
        }
    }

    /**
     * 返回声明站点成员 scope。
     */
    override fun getDeclarationSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope {
        val classSymbol = klass.symbol as? CfirClassSymbol ?: return CfirTypeScope.Empty
        return getDeclarationSiteMemberScope(classSymbol, useSiteSession, scopeSession)
    }

    /**
     * 返回 class、interface、struct 或 enum 声明站点的成员 scope；typealias 使用专用构造器 scope。
     */
    fun getDeclarationSiteMemberScope(
        classSymbol: CfirClassLikeSymbol<*>,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope {
        check(classSymbol !is CfirTypeAliasSymbol) {
            "Type aliases do not support a nominal declaration-site member scope; constructor lookup uses its dedicated scope."
        }
        return getUseSiteMemberScope(
            useSiteSession = useSiteSession,
            classSymbol = classSymbol,
            scopeSession = scopeSession,
            symbolProvider = useSiteSession.symbolProvider,
            extendProvider = useSiteSession.extendProvider,
            directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
            scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
            memberScopeView = CfirCjmpMemberScopeView.RAW,
            memberRequiredPhase = CfirResolvePhase.STATUS,
        )
    }

    /**
     * 返回包成员 scope。
     */
    fun getPackageMemberScope(
        packageFqName: FqName,
        symbolProvider: CfirSymbolProvider,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirPackageScope {
        val key: ScopeSessionKey<PackageMemberScopeKey, CfirPackageMemberScope> = scopeSessionKey()
        return scopeSession.getOrBuild(PackageMemberScopeKey(packageFqName, symbolProvider, useSiteSession), key) {
            CfirPackageMemberScope(packageFqName, useSiteSession, symbolProvider)
        }
    }

    /**
     * 包成员 scope 缓存 key。
     *
     * @property packageFqName 被查询的包名。
     * @property symbolProvider 查询发生时使用的符号提供器。
     * @property useSiteSession 查询发生的 session。
     */
    private data class PackageMemberScopeKey(
        /**
         * 被查询的包名。
         */
        val packageFqName: FqName,
        /**
         * 查询发生时使用的符号提供器；必须进入 key，否则同一 session 下首次构建的 scope 会错用另一个提供器。
         */
        val symbolProvider: CfirSymbolProvider,
        /**
         * 查询发生的 use-site session。
         */
        val useSiteSession: CfirSession,
    )
}


/**
 * typealias constructor scope 的 ScopeSession key。
 */
private data class TypeAliasConstructorScopeKey(
    val alias: CfirTypeAliasSymbol,
    val sourceTypeRef: CfirTypeRef,
    val expandedType: ConeLookupTagBasedType,
    val view: CfirCjmpMemberScopeView,
    val parentScope: CfirClassSubstitutionScope,
)

private val GENERIC_INSTANTIATION_MEMBER_SCOPES:
        ScopeSessionKey<ScopeSessionOwnedId<CfirPreparedScopeProduct<Pair<CfirClassUseSiteMemberScope, CfirClassSubstitutionScope>>>, CfirPreparedScopeProduct<CfirCangJieScopeProvider.GenericInstantiationMemberScopes>> =
    scopeSessionKey()

private val TYPEALIAS_CONSTRUCTOR: ScopeSessionKey<ScopeSessionOwnedId<TypeAliasConstructorScopeKey>, CfirPreparedScopeProduct<CfirScope>> =
    scopeSessionKey()

/**
 * 返回 class-like 声明未经外部类型替换的成员 scope。
 */
fun CfirClassLikeDeclaration.unsubstitutedScope(
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean,
    memberRequiredPhase: CfirResolvePhase?,
): CfirTypeScope {
    val memberScopeView = useSiteMemberScopeView(useSiteSession, memberRequiredPhase)
    val scope = when (this) {
        is CfirClass -> if (memberScopeView == CfirCjmpMemberScopeView.RAW) {
            scopeProvider.getUseSiteMemberScope(this, useSiteSession, scopeSession, memberRequiredPhase)
        } else {
            useSiteMemberScopeInView(useSiteSession, scopeSession, memberScopeView, memberRequiredPhase)
        }
        else -> {
            val symbol = symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
            useSiteSession.cangjieScopeProvider.getUseSiteMemberScope(
                classSymbol = symbol,
                useSiteSession = useSiteSession,
                scopeSession = scopeSession,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
                memberRequiredPhase = memberRequiredPhase,
            )
        }
    }
    if (withForcedTypeCalculator) return CfirScopeWithCallableCopyReturnTypeUpdater(scope, CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution)
    return scope
}

private fun useSiteMemberScopeView(
    useSiteSession: CfirSession,
    memberRequiredPhase: CfirResolvePhase?,
): CfirCjmpMemberScopeView = if (
    memberRequiredPhase != null && memberRequiredPhase >= CfirResolvePhase.CJMP_MATCHING &&
    useSiteSession.isCjmpSpecificCompilationEnabled()
) {
    CfirCjmpMemberScopeView.CJMP_ACTUALIZED
} else {
    CfirCjmpMemberScopeView.RAW
}

private fun CfirClass.useSiteMemberScopeInView(
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    memberScopeView: CfirCjmpMemberScopeView,
    memberRequiredPhase: CfirResolvePhase?,
): CfirTypeScope {
    val provider = useSiteSession.cangjieScopeProvider
    fun prepareRaw(): CfirClassUseSiteMemberScope = provider.getUseSiteMemberScope(
        classSymbol = symbol,
        useSiteSession = useSiteSession,
        scopeSession = scopeSession,
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
        memberScopeView = memberScopeView,
        memberRequiredPhase = memberRequiredPhase,
    )
    return provider.getClassSubstitutionScope(useSiteSession, scopeSession, ::prepareRaw, symbol.constructType()).value
}

/** scope 的阶段要求是整个成员层次的产品保障，与缓存中的原始/实际化视图身份一起维护。 */





internal fun createMemberDeclarationSubstitutor(
    symbol: CfirClassLikeSymbol<*>,
    type: ConeCangJieType,
    session: CfirSession,
): ConeSubstitutor? {
        if (type !is ConeLookupTagBasedType) return null
        if (!symbol.isBound || symbol.cfir.typeParameters.isEmpty()) return null
        if (symbol.cfir.typeParameters.size != type.typeArguments.size) return null

        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            symbol.cfir.typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() to argument.type
            }
        return replacements.takeIf { it.isNotEmpty() }?.let { createTypeSubstitutorByTypeConstructor(it, session.typeContext, false) }
}
