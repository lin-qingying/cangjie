package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.hasGenericNode
import org.cangnova.cangjie.cfir.scopes.CfirInheritanceMemberHeader
import org.cangnova.cangjie.cfir.scopes.CfirInheritanceMemberKind
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationInheritanceScope
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationInheritanceInput
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationInheritanceStep
import org.cangnova.cangjie.cfir.scopes.CfirMappedInheritedMember
import java.util.Collections
import org.cangnova.cangjie.cfir.session.extendRuleQueryService
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerDisposition
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeSources
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendMemberPeerPreparationInput
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationPackage
import org.cangnova.cangjie.cfir.resolve.providers.isBareOrDeclarationSelfTypeOf
import org.cangnova.cangjie.cfir.resolve.providers.prepareExtendMemberTypeMapping
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.CfirFunctionInstantiationScope
import org.cangnova.cangjie.cfir.scopes.CfirFunctionInstantiationTable
import org.cangnova.cangjie.cfir.scopes.CfirFunctionMemberTableKind
import org.cangnova.cangjie.cfir.scopes.CfirMemberScopeOwner
import org.cangnova.cangjie.cfir.scopes.CfirPotentialDirectExtendCallableScope
import org.cangnova.cangjie.cfir.scopes.CfirStoredFunctionMember
import org.cangnova.cangjie.cfir.scopes.instantiationExtendOwners
import org.cangnova.cangjie.cfir.scopes.instantiationRelatedPreparedMemberExtends
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerFacts
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.declarationTypeArguments
import org.cangnova.cangjie.cfir.types.extendTargetKey
import org.cangnova.cangjie.cfir.types.generateGenericTypeMapping
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenanceScope
import org.cangnova.cangjie.cfir.scopes.CfirCallableWithLookupProvenance
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpMemberSnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.idealExtendLookupTypes
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * extend 成员 type scope。
 *
 * 官方 extend map 同时支持 nominal declaration 与 built-in type。这里以
 * [CfirExtendTargetKey] 为索引键，让 `CPointer<T>` / `CString` 这类没有
 * ClassId 的 built-in target 也能进入和普通类型相同的成员查询流程。
 */
class CfirExtendMemberScope internal constructor(
    /**
     * 当前 scope 查询的 extend 目标 key，可以是 class-like 或 non-class built-in 类型。
     */
    private val targetKey: CfirExtendTargetKey,
    /**
     * 提供 extend 索引、可见性和包归属信息的 provider。
     */
    private val extendProvider: CfirExtendProvider,
    /**
     * 当前 use-site session。
     */
    private val session: CfirSession,
    /** 继承发布表与父 scope 生产沿用调用方的缓存会话。 */
    private val scopeSession: ScopeSession,
    /** provider 已完成的候选、声明及 CJMP 快照；构造器不再执行解析回调。 */
    private val scopeInput: ScopeInput,
    /**
     * 成员查找时的实际 receiver 类型。
     */
    private val receiverType: ConeCangJieType,
    /**
     * 是否允许裸泛型静态 qualifier 暂缓 extend 泛型适用性判断。
     */
    private val allowBareGenericStaticQualifierExtends: Boolean = false,
    /**
     * 构造 extend 体内成员 scope 时需要排除的当前 extend 声明。
     */
    private val excludingExtend: CfirExtend? = null,
    /** 当前 scope 图的 owner 视角，负责直接 extend 来源和 private owner 边界。 */
    private val scopeOwner: CfirMemberScopeOwner = CfirMemberScopeOwner.UseSite,
    /** body/use-site scope 可消费 CJMP 实际化成员图；前置阶段始终保留原始声明。 */
    private val memberScopeView: CfirCjmpMemberScopeView = CfirCjmpMemberScopeView.RAW,
    /** 普通查找无此事实；实例化发布表必须由实际 nominal/extend 声明给出 owner。 */
    private val instantiationOwner: CfirDeclaration? = scopeOwner.extendOwner,
) : CfirTypeScope(), CfirCjmpMemberSubstitutorProvider, CfirFunctionInheritanceScope, CfirFunctionInstantiationScope,
    CfirCallableLookupProvenanceScope, CfirDeclarationInheritanceScope, CfirPotentialDirectExtendCallableScope {

    /** 一个实际 extend owner 的冻结成员输入，包含参数上界和原始来源引用。 */
    internal data class OwnerInput(
        val declarations: List<CfirDeclaration>,
        val snapshot: CfirCjmpMemberSnapshot?,
        val targetRef: CfirTypeRef,
        val superTypeRefs: List<CfirTypeRef>,
        val parameters: List<Pair<CfirTypeParameter, List<CfirTypeRef>>>,
        val members: List<CfirDeclaration>,
        val memberHeaders: Map<CfirBasedSymbol<*>, CfirInheritanceMemberHeader>,
    )

    /** 可审计的替换值；运行时 substitutor 不进入输入相等性。 */
    internal data class SubstitutionInput(val replacements: Map<TypeConstructorMarker, ConeCangJieType>, val receiver: ConeCangJieType)
    internal enum class Applicability { EXACT, INAPPLICABLE, DEFERRED_BARE }
    internal data class ReceiverKey(val owner: CfirExtend, val receiver: ConeCangJieType)
    internal data class ReceiverInput(
        val applicability: Applicability,
        val directSubstitution: SubstitutionInput?,
        val ownerSubstitution: SubstitutionInput?,
        val memberSubstitutions: Map<CfirBasedSymbol<*>, SubstitutionInput>,
    )

    /** 当前 owner 检查域中的 peer 输入；结构映射和实例化可见性是独立事实。 */
    internal data class PeerOwnerInput(
        val visible: Boolean,
        val inRootInheritRelation: Boolean,
        val substitution: SubstitutionInput,
        val members: List<CfirDeclaration>,
        val memberSubstitutions: Map<CfirBasedSymbol<*>, SubstitutionInput>,
        val interfaces: List<Pair<CfirTypeRef, ConeCangJieType>>,
    )

    /** 完整有序候选与所有实际 owner 的成员产品，供 provider canonicalize 后注入。 */
    internal data class ScopeInput(
        val sources: CfirTypeAwareSupertypeSources,
        val structuralProvider: CfirTypeAwareSupertypeProvider?,
        val candidates: List<ExtendLookupCandidate>,
        val owners: Map<CfirExtend, OwnerInput>,
        val inheritedInterfaces: Map<CfirExtend, List<InheritedInterfaceInput>>,
        val nominalScope: CfirTypeScope?,
        val receivers: Map<ReceiverKey, ReceiverInput>,
        val instantiationOwners: Set<CfirExtend>,
        val matchedPeers: Map<CfirExtend, Set<CfirExtend>>,
        val peerDispositions: Map<Pair<CfirExtend, CfirExtend>, CfirExtendMemberPeerDisposition>,
        val peerFacts: Map<Pair<CfirExtend, CfirExtend>, CfirExtendMemberPeerFacts>,
        val peerOwners: Map<Pair<CfirExtend, CfirExtend>, PeerOwnerInput>,
        val peerInheritedInterfaces: Map<Pair<CfirExtend, CfirExtend>, List<InheritedInterfaceInput>>,
        val nominalInheritanceMembers: List<CfirMappedInheritedMember> = emptyList(),
        val eligiblePeers: Map<CfirExtend, List<CfirExtend>> = emptyMap(),
    )

    /** provider 已完成的每条接口来源及其父成员图，不在名字查询期间重新生产父树。 */
    internal data class InheritedInterfaceInput(
        val sourceTypeRef: CfirTypeRef,
        val type: ConeCangJieType,
        val scope: CfirTypeScope,
        val inheritanceMembers: List<CfirMappedInheritedMember>,
    )

    companion object {
        /** 每次正式准备都读取真实索引和已发布快照；null phase 不推进 callable 阶段。 */
        internal fun prepareScopeInput(
            targetKey: CfirExtendTargetKey,
            extendProvider: CfirExtendProvider,
            session: CfirSession,
            receiverType: ConeCangJieType,
            memberScopeView: CfirCjmpMemberScopeView,
            requiredPhase: CfirResolvePhase?,
            scopeOwner: CfirMemberScopeOwner,
            instantiationOwner: CfirDeclaration?,
            sources: CfirTypeAwareSupertypeSources,
            structuralProvider: CfirTypeAwareSupertypeProvider?,
            allowBare: Boolean,
            aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?,
            registerParameterOwner: (org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol) -> Unit,
        ): ScopeInput {
            val candidates = extendsForTarget(targetKey, extendProvider, receiverType)
            val owners = linkedSetOf<CfirExtend>().apply {
                candidates.forEach { add(it.extend) }
                scopeOwner.extendOwner?.let(::add)
                (instantiationOwner as? CfirExtend)?.let(::add)
                (scopeOwner as? CfirMemberScopeOwner.ExtendInstantiation)?.relatedExtends?.let(::addAll)
            }
            if (requiredPhase != null) owners.forEach { it.lazyResolveToPhaseRecursively(requiredPhase) }
            owners.forEach { it.lazyResolveToPhaseRecursively(CfirResolvePhase.TYPES) }

            fun capture(owner: CfirExtend): OwnerInput {
                val snapshot = if (memberScopeView == CfirCjmpMemberScopeView.RAW ||
                    !session.isCjmpSpecificCompilationEnabled() || !owner.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)
                ) null else if (memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) {
                    session.cjmpFamilyAggregate.matchingGroupFor(owner)
                } else if (session.requiresDeferredCjmpFinalMatching()) {
                    session.cjmpFamilyAggregate.structureFor(owner)
                } else {
                    session.cjmpFamilyAggregate.matchingGroupFor(owner)
                }
                val members = (if (snapshot == null) owner.declarations
                    else if (memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) snapshot.mergedDeclarationsFor(owner)
                    else snapshot.membersFor(owner)).toList()
                val headers = members.mapNotNull { member ->
                    if (member is CfirConstructor) return@mapNotNull null
                    val physical = snapshot?.physicalOwnerFor(member) ?: owner
                    val logical = snapshot?.logicalOwnerFor(member) ?: owner
                    captureInheritanceMemberHeader(member, physical, logical,
                        { it.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion) }, registerParameterOwner)
                        ?.let { it.declaration to it }
                }.toMap()
                return OwnerInput(
                    Collections.unmodifiableList(owner.declarations.toList()), snapshot, owner.extendedTypeRef,
                    Collections.unmodifiableList(owner.superTypeRefs.toList()),
                    Collections.unmodifiableList(owner.typeParameters.map { it to Collections.unmodifiableList(it.bounds.toList()) }),
                    Collections.unmodifiableList(members), Collections.unmodifiableMap(headers),
                )
            }
            while (true) {
                val first = owners.associateWith(::capture)
                val completed = owners.associateWith(::capture)
                // 后一个 snapshot 回调也可能发布前一个 owner 的产品，不能发布混合视图。
                if (first != completed) continue
                val logicalOwners = completed.values.flatMap { input -> input.members.mapNotNull { member ->
                    input.snapshot?.logicalOwnerFor(member) as? CfirExtend
                } }
                val bucketOwners = if (memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) completed.values.flatMap { input ->
                    val type = input.targetRef.coneTypeOrNull?.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                    type?.extendTargetKey?.let { key -> extendsForTarget(key, extendProvider, type).map { it.extend } }.orEmpty()
                } else emptyList()
                if (owners.addAll(logicalOwners + bucketOwners)) {
                    if (requiredPhase != null) owners.forEach { it.lazyResolveToPhaseRecursively(requiredPhase) }
                    owners.forEach { it.lazyResolveToPhaseRecursively(CfirResolvePhase.TYPES) }
                    continue
                }
                fun immutableMap(values: Map<TypeConstructorMarker, ConeCangJieType>): Map<TypeConstructorMarker, ConeCangJieType> =
                    Collections.unmodifiableMap(LinkedHashMap(values))
                fun captured(owner: CfirExtend, substitution: org.cangnova.cangjie.cfir.resolve.providers.CfirExtendDeclarationSubstitution?): SubstitutionInput? =
                    substitution?.let { value -> SubstitutionInput(immutableMap(completed.getValue(owner).parameters.associate { (parameter, _) ->
                        parameter.symbol.toLookupTag() to value.substitutor.substituteOrSelf(parameter.symbol.constructType())
                    }), value.substitutedReceiverType) }
                fun runtime(value: SubstitutionInput): ConeSubstitutor =
                    createTypeSubstitutorByTypeConstructor(value.replacements, session.typeContext, false)
                val receiverTypes = (listOf(receiverType) + candidates.map { it.concreteReceiverType }).distinct()
                val receivers = linkedMapOf<ReceiverKey, ReceiverInput>()
                for (owner in owners) for (receiver in receiverTypes) {
                    val input = completed.getValue(owner)
                    val pattern = input.targetRef.coneTypeOrNull
                    val direct = pattern?.let { createExtendDeclarationSubstitution(session, owner, it, receiver, sources.symbols, aliasExpansion) }
                    val receiverOwner = (receiver as? ConeLookupTagBasedType)?.lookupTag?.toSymbol(session, sources.symbols) as? CfirClassLikeSymbol<*>
                    val deferred = direct == null && allowBare && receiverOwner != null && pattern is ConeLookupTagBasedType && receiver is ConeLookupTagBasedType &&
                        pattern.typeArguments.isNotEmpty() && pattern.classIdOrPrimitiveClassId == receiver.classIdOrPrimitiveClassId &&
                        receiver.isBareOrDeclarationSelfTypeOf(receiverOwner)
                    val state = if (direct != null) Applicability.EXACT else if (deferred) Applicability.DEFERRED_BARE else Applicability.INAPPLICABLE
                    val ownerMapping = captured(owner, findExtendDeclarationSubstitution(session, owner, receiver, sources.symbols, structuralProvider, aliasExpansion))
                    val members = linkedMapOf<CfirBasedSymbol<*>, SubstitutionInput>()
                    for (member in input.members) {
                        val projection = input.snapshot?.ownerTypeParameterMappingFor(member).orEmpty()
                        val logical = input.snapshot?.logicalOwnerFor(member) as? CfirExtend ?: owner
                        val mapped = captured(logical, findExtendDeclarationSubstitution(session, logical, receiver, sources.symbols, structuralProvider, aliasExpansion))
                        if (mapped == null && state == Applicability.INAPPLICABLE) continue
                        check(mapped != null || state == Applicability.DEFERRED_BARE) {
                            "An exact member requires its prepared logical owner mapping"
                        }
                        val values = if (mapped != null) LinkedHashMap(mapped.replacements) else linkedMapOf<TypeConstructorMarker, ConeCangJieType>().apply {
                            // 延后调用只保留声明模板；没有 receiver 匹配成功的承诺。
                            completed.getValue(logical).parameters.forEach { (parameter, _) -> put(parameter.symbol.toLookupTag(), parameter.symbol.constructType()) }
                        }
                        if (projection.isNotEmpty()) {
                            check(mapped != null || state == Applicability.DEFERRED_BARE) { "A projected member requires its logical owner's completed receiver mapping" }
                            val substitutor = mapped?.let(::runtime)
                            projection.forEach { (physical, target) -> values[physical.symbol.toLookupTag()] =
                                substitutor?.substituteOrSelf(target.symbol.constructType()) ?: target.symbol.constructType() }
                        }
                        members[member.symbol] = SubstitutionInput(immutableMap(values), mapped?.receiver ?: receiver)
                    }
                    receivers[ReceiverKey(owner, receiver)] = ReceiverInput(state, captured(owner, direct), ownerMapping,
                        Collections.unmodifiableMap(members))
                }
                val replaced = completed.values.flatMap { input -> input.snapshot?.let { snapshot ->
                    completed.keys.flatMap { snapshot.commonCounterpartsFor(it) }.filterIsInstance<CfirExtend>()
                }.orEmpty() }.toSet()
                val instantiationOwners = owners.filterNot { it in replaced }.toSet()
                val peers = linkedMapOf<CfirExtend, Set<CfirExtend>>()
                val dispositions = linkedMapOf<Pair<CfirExtend, CfirExtend>, CfirExtendMemberPeerDisposition>()
                val peerFacts = linkedMapOf<Pair<CfirExtend, CfirExtend>, CfirExtendMemberPeerFacts>()
                val peerOwners = linkedMapOf<Pair<CfirExtend, CfirExtend>, PeerOwnerInput>()
                for (owner in owners) {
                    val ownerTarget = completed.getValue(owner).targetRef.coneTypeOrNull
                    val semanticTarget = ownerTarget?.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                    val bucket = semanticTarget?.extendTargetKey?.let { key ->
                        extendsForTarget(key, extendProvider, semanticTarget).map { it.extend }.toSet()
                    }.orEmpty()
                    val matched = owners.filter { peer -> peer === owner ||
                        if (memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) peer in bucket
                        else ownerTarget?.let { receiver -> completed.getValue(peer).targetRef.coneTypeOrNull?.let { pattern ->
                            createExtendDeclarationSubstitution(session, peer, pattern, receiver, sources.symbols, aliasExpansion)
                        } } != null }.toSet()
                    peers[owner] = Collections.unmodifiableSet(matched)
                    if (memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION) for (peer in matched) {
                        val ownerInput = completed.getValue(owner)
                        val peerInput = completed.getValue(peer)
                        val input = CfirExtendMemberPeerPreparationInput(sources, checkNotNull(structuralProvider),
                            ownerInput.targetRef.coneTypeOrNull, peerInput.targetRef.coneTypeOrNull,
                            ownerInput.superTypeRefs.mapNotNull { it.coneTypeOrNull },
                            peerInput.superTypeRefs.mapNotNull { it.coneTypeOrNull },
                            ownerInput.parameters.map { it.first.symbol },
                            ownerInput.parameters.associate { it.first.symbol to it.second.mapNotNull { bound -> bound.coneTypeOrNull } },
                            aliasExpansion)
                        val facts = session.extendRuleQueryService.prepareExtendMemberPeerFacts(owner, peer, input)
                        peerFacts[owner to peer] = facts
                        dispositions[owner to peer] = session.extendRuleQueryService.extendMemberPeerDecision(facts).disposition
                        // inactive 与约束不可见分开：只对真实有资格的 peer 准备成员与接口图。
                        if (peer !== owner && (peer !in instantiationOwners ||
                            peer !in session.instantiationRelatedPreparedMemberExtends(owner, listOf(peer), CfirAccessContext(
                                useSiteFile = owner.getContainingFile(), containingDeclarations = listOf(owner), receiverType = receiverType,
                                lookupOrigin = CfirLookupOrigin.MEMBER, kind = CfirAccessKind.EXTEND,
                            )) {
                                dispositions.getValue(owner to it)
                            })) continue
                        if (ownerTarget == null || ownerTarget.containsErrorType() ||
                            peerInput.targetRef.coneTypeOrNull == null || peerInput.targetRef.coneTypeOrNull?.containsErrorType() == true) continue
                        val destination = checkNotNull(ownerTarget).fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        val pattern = checkNotNull(peerInput.targetRef.coneTypeOrNull)
                            .fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        val structural = prepareExtendMemberTypeMapping(session, pattern, destination, sources.symbols, aliasExpansion)
                        val projected = createTypeSubstitutorByTypeConstructor(structural, session.typeContext, false)
                        val mapping = SubstitutionInput(structural, projected.substituteOrSelf(pattern))
                        val visible = createExtendDeclarationSubstitution(session, peer, pattern, destination, sources.symbols, aliasExpansion) != null
                        val memberMappings = peerInput.members.associate { member ->
                            val logical = peerInput.snapshot?.logicalOwnerFor(member) as? CfirExtend ?: peer
                            val logicalPattern = checkNotNull(completed.getValue(logical).targetRef.coneTypeOrNull)
                            val values = LinkedHashMap(prepareExtendMemberTypeMapping(session, logicalPattern, destination, sources.symbols, aliasExpansion))
                            val logicalSubstitutor = createTypeSubstitutorByTypeConstructor(values, session.typeContext, false)
                            peerInput.snapshot?.ownerTypeParameterMappingFor(member).orEmpty().forEach { (physical, target) ->
                                values[physical.symbol.toLookupTag()] = logicalSubstitutor.substituteOrSelf(target.symbol.constructType())
                            }
                            member.symbol to SubstitutionInput(immutableMap(values), logicalSubstitutor.substituteOrSelf(logicalPattern))
                        }
                        val interfaces = peerInput.superTypeRefs.mapNotNull { ref -> ref.coneTypeOrNull?.let { type ->
                            ref to projected.substituteOrSelf(type).fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        } }
                        val relationMapping = generateGenericTypeMapping(destination, pattern, session.typeContext) {
                            it.fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        }
                        val leftInterfaces = ownerInput.superTypeRefs.mapNotNull { it.coneTypeOrNull }.map {
                            relationMapping.substituteOrSelf(it).fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        }
                        val rightInterfaces = peerInput.superTypeRefs.mapNotNull { it.coneTypeOrNull }.map {
                            relationMapping.substituteOrSelf(it).fullyExpandedTypeUsingAbbreviation(session, sources.symbols, aliasExpansion)
                        }
                        val related = leftInterfaces.any { left -> rightInterfaces.any { right ->
                            AbstractTypeChecker.isSubtypeOf(session.typeContext, left, right) ||
                                AbstractTypeChecker.isSubtypeOf(session.typeContext, right, left)
                        } }
                        peerOwners[owner to peer] = PeerOwnerInput(visible, related, mapping, peerInput.members,
                            Collections.unmodifiableMap(memberMappings), Collections.unmodifiableList(interfaces))
                    }
                }
                if (owners.associateWith(::capture) != completed) continue
                return ScopeInput(sources, structuralProvider, Collections.unmodifiableList(candidates.toList()),
                    Collections.unmodifiableMap(completed), emptyMap(), null, Collections.unmodifiableMap(receivers),
                    Collections.unmodifiableSet(instantiationOwners), Collections.unmodifiableMap(peers), Collections.unmodifiableMap(dispositions),
                    Collections.unmodifiableMap(peerFacts), Collections.unmodifiableMap(peerOwners), emptyMap(),
                    eligiblePeers = Collections.unmodifiableMap(owners.associateWith { owner ->
                        Collections.unmodifiableList(peerOwners.keys.filter { it.first === owner }.map { it.second })
                    }))
            }
        }

        /** 扩展来源先完成自身声明及成员的阶段，复制工厂只消费完成后的签名。 */
        internal fun ensureRequiredMembersPhase(
            targetKey: CfirExtendTargetKey,
            extendProvider: CfirExtendProvider,
            receiverType: ConeCangJieType,
            memberRequiredPhase: CfirResolvePhase?,
        ) {
            if (memberRequiredPhase == null) return
            for (candidate in extendsForTarget(targetKey, extendProvider, receiverType)) {
                candidate.extend.lazyResolveToPhaseRecursively(memberRequiredPhase)
            }
        }

        private fun extendsForTarget(
            targetKey: CfirExtendTargetKey,
            extendProvider: CfirExtendProvider,
            receiverType: ConeCangJieType,
        ): List<ExtendLookupCandidate> {
            val result = mutableListOf<ExtendLookupCandidate>()
            val primitiveKind = targetKey.classIdOrNull?.toPrimitiveTypeKindOrNull()
            if (primitiveKind != null) {
                // primitive 的真实 receiver 先于 synthetic class-like 索引，来源与适用性共用此序列。
                val concreteReceiverTypes = receiverType.idealExtendLookupTypes.ifEmpty {
                    listOf(ConePrimitiveType(primitiveKind))
                }
                for (concreteReceiverType in concreteReceiverTypes) {
                    result += extendProvider.getExtendsForBuiltinType(concreteReceiverType.kind).map {
                        ExtendLookupCandidate(it, concreteReceiverType)
                    }
                }
            }
            result += extendProvider.getExtendsForTarget(targetKey).map {
                ExtendLookupCandidate(it, receiverType)
            }
            return result.distinctBy { it.extend }
        }
    }

    private val memberSubstitutors = hashMapOf<Pair<CfirBasedSymbol<*>, ConeCangJieType>, ConeSubstitutor>()

    override fun cjmpMemberOwnerFor(symbol: CfirCallableSymbol<*>): CfirCjmpMemberOwnerProjection? {
        val original = symbol.unwrapSubstitutionOverrides()
        val projections = scopeInput.owners.values.mapNotNull { input -> input.memberHeaders[original]?.let { header ->
            CfirCjmpMemberOwnerProjection(checkNotNull(header.physicalOwner), checkNotNull(header.logicalOwner))
        } }.distinct()
        check(projections.size <= 1) { "A prepared extend member has conflicting logical owner projections" }
        return projections.singleOrNull()
    }

    override fun cjmpOwnerSubstitutorFor(symbol: CfirCallableSymbol<*>): ConeSubstitutor? {
        val declaration = symbol.unwrapSubstitutionOverrides().cfir
        val values = scopeInput.receivers.filterKeys { it.receiver == receiverType && it.owner in scopeInput.instantiationOwners }.values
            .mapNotNull { it.memberSubstitutions[declaration.symbol] }.distinct()
        check(values.size <= 1) { "A member has conflicting prepared owner substitutions" }
        val input = values.singleOrNull() ?: return null
        return memberSubstitutors.getOrPut(declaration.symbol to receiverType) {
            createTypeSubstitutorByTypeConstructor(input.replacements, session.typeContext, false)
        }
    }

    /**
     * 当前目标可见 extend 成员索引。
     */
    private val memberIndex: MemberIndex by lazy { buildIndex() }

    /**
     * 返回 extend 成员 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = buildSet {
        addAll(memberIndex.functions.keys)
        addAll(memberIndex.properties.keys)
        addAll(memberIndex.variables.keys)
    }

    /**
     * 返回 extend 成员 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = memberIndex.classifiers.keys

    /**
     * 按名称处理 extend classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    /**
     * 按名称处理 extend 函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
    }

    /** 暴露当前 owner 组的直接函数及其物理声明来源。 */
    override fun processFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    ) {
        processFunctionsByName(name) { member ->
            val original = member.unwrapSubstitutionOverrides() as CfirNamedFunctionSymbol
            val ownerType = original.getContainingExtend()?.extendedTypeRef?.coneTypeOrNull
            val sourceExtend = original.getContainingExtend()
            processor(
                CfirFunctionInheritanceProvenance(
                    member = member,
                    identity = CfirFunctionInheritanceIdentity(original, ownerType),
                    baseScope = this,
                    lookupProvenance = sourceExtend?.let(CfirCallableLookupProvenance::directExtendMember)
                        ?: CfirCallableLookupProvenance.None,
                ),
            )
        }
    }

    private val publishedInstantiationFunctions = hashMapOf<Name, CfirFunctionInstantiationTable>()
    private val instantiationFunctions = hashMapOf<Name, CfirFunctionInstantiationTable>()
    private val declarationInheritanceInputs = hashMapOf<CfirExtend, CfirDeclarationInheritanceInput>()

    /** 两张 parent 表按 owner 域独立归并，步骤在当前声明写入 visible 之前截取。 */
    override fun declarationInheritanceInput(owner: CfirExtend): CfirDeclarationInheritanceInput =
        declarationInheritanceInputs.getOrPut(owner) {
            check(memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION)
            val ownerInput = scopeInput.owners.getValue(owner)
            val relatedExtends = scopeInput.peerOwners.filter { (pair, input) -> pair.first === owner && input.inRootInheritRelation }
                .keys.map { it.second }.toSet()
            val visible = CfirDeclarationInheritanceTableBuilder(session, owner, relatedExtends)
            val invisible = CfirDeclarationInheritanceTableBuilder(session, owner, relatedExtends)
            val visibleDeclarations = CfirDeclarationInheritanceTableBuilder(session, owner, relatedExtends)
            val invisibleDeclarations = CfirDeclarationInheritanceTableBuilder(session, owner, relatedExtends)
            val peers = scopeInput.eligiblePeers.getValue(owner)
            for (peer in peers) {
                if (peer === owner) continue
                val input = scopeInput.peerOwners.getValue(owner to peer)
                val interfaces = if (input.visible) visible else invisible
                val declared = if (input.visible) visibleDeclarations else invisibleDeclarations
                interfaces.addAll(mappedInterfaceMembers(scopeInput.peerInheritedInterfaces.getValue(owner to peer), owner))
                val peerHeaders = scopeInput.owners.getValue(peer).memberHeaders
                for (member in input.members) {
                    val header = peerHeaders[member.symbol] ?: continue
                    if (header.flags.visibility == Visibilities.Private ||
                        header.flags.visibility != Visibilities.Protected &&
                        !session.accessibilityChecker.isCallableVisibleFromPackage(header.declaration, owner.getDeclarationPackage())) continue
                    val mapping = input.memberSubstitutions.getValue(header.declaration)
                    val substitutor = createTypeSubstitutorByTypeConstructor(mapping.replacements, session.typeContext, false)
                    declared.update(header.bindInheritanceMember(this, mapping.receiver, substitutor, peer,
                        CfirCallableLookupProvenance.directExtendMember(peer)))
                }
            }
            visible.addAll(visibleDeclarations.snapshot())
            invisible.addAll(invisibleDeclarations.snapshot())
            visible.removeAbstractMembers()
            invisible.removeAbstractMembers()
            visible.addAll(scopeInput.nominalInheritanceMembers)
            val initialVisible = visible.snapshot()
            val requirements = mappedInterfaceMembers(scopeInput.inheritedInterfaces.getValue(owner), owner)
            val own = scopeInput.peerOwners.getValue(owner to owner)
            val steps = ArrayList<CfirDeclarationInheritanceStep>()
            for (member in ownerInput.members) {
                val header = ownerInput.memberHeaders[member.symbol] ?: continue
                val mapping = own.memberSubstitutions.getValue(header.declaration)
                val substitutor = createTypeSubstitutorByTypeConstructor(mapping.replacements, session.typeContext, false)
                val child = header.bindInheritanceMember(this, mapping.receiver, substitutor, null,
                    CfirCallableLookupProvenance.directExtendMember(owner))
                steps += CfirDeclarationInheritanceStep(child, visible.snapshot().filter { it.name == child.name },
                    invisible.snapshot().filter { it.name == child.name })
                visible.update(child)
            }
            val sources = scopeInput.inheritedInterfaces.getValue(owner).flatMap { input ->
                input.inheritanceMembers.map { it to input.sourceTypeRef }
            }
            val final = CfirDeclarationInheritanceTableBuilder(session, owner, relatedExtends)
            final.addAll(requirements)
            final.addAll(visible.snapshot())
            CfirDeclarationInheritanceInput(owner, steps, requirements, visible.snapshot(), invisible.snapshot(),
                requirements.associateWith { requirement -> sources.first { it.first.declaration === requirement.declaration }.second },
                initialVisible, final.snapshot())
        }

    private fun mappedInterfaceMembers(inputs: List<InheritedInterfaceInput>, owner: CfirExtend): List<CfirMappedInheritedMember> {
        val relatedExtends = scopeInput.peerOwners.filter { (pair, input) -> pair.first === owner && input.inRootInheritRelation }
            .keys.map { it.second }.toSet()
        val builder = CfirDeclarationInheritanceTableBuilder(session, owner, relatedExtends)
        inputs.forEach { builder.addAll(it.inheritanceMembers, true) }
        return builder.snapshot()
    }


    override fun publishedFunctionMemberTable(name: Name): CfirFunctionInstantiationTable {
        check(memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION)
        return publishedInstantiationFunctions.getOrPut(name) {
            val owner = checkNotNull(instantiationOwner) { "A stored extend table requires its actual declaration owner" }
            val currentAccessContext = owner.instantiationAccessContext()
            val extendOwner = (scopeOwner as? CfirMemberScopeOwner.ExtendInstantiation)?.extendOwner
            if (extendOwner != null) {
                buildPublishedOwnerTable(name, extendOwner, (scopeOwner as CfirMemberScopeOwner.ExtendInstantiation).relatedExtends)
            } else {
                combinePublishedOwnerTables(name, currentAccessContext)
            }
        }
    }

    override fun inheritedFunctionMemberTable(name: Name, useSiteOwner: CfirDeclaration): CfirFunctionInstantiationTable =
        combinePublishedOwnerTables(name, useSiteOwner.instantiationAccessContext())

    /** GetInheritedSuperMembers 逐个将 extend 写入已有 class 表，不能先把所有 extend 相互归并。 */
    internal fun processPublishedOwnerTablesForInheritance(
        name: Name,
        useSiteOwner: CfirDeclaration,
        processor: (CfirFunctionInstantiationTable) -> Unit,
    ) {
        processPublishedOwnerTables(name, useSiteOwner.instantiationAccessContext(), processor)
    }

    /** 各实际 extend 的发布结果按来源输入序归并，访问判定使用继承它的真实声明文件。 */
    private fun combinePublishedOwnerTables(name: Name, accessContext: CfirAccessContext): CfirFunctionInstantiationTable {
        val builder = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
        processPublishedOwnerTables(name, accessContext) { builder.addTable(it) }
        return builder.finish(CfirFunctionMemberTableKind.VISIBLE_EXTEND)
    }

    private fun processPublishedOwnerTables(
        name: Name,
        accessContext: CfirAccessContext,
        processor: (CfirFunctionInstantiationTable) -> Unit,
    ) {
        for (candidate in selectedInstantiationExtends()) {
            val extend = candidate.extend
            if (session.accessibilityChecker.checkExtend(extend, accessContext) !is CfirAccessibilityResult.Accessible) continue
            val related = session.instantiationRelatedPreparedMemberExtends(extend,
                scopeInput.eligiblePeers.getValue(extend).filter { scopeInput.peerOwners.getValue(extend to it).visible },
                accessContext) { peer -> scopeInput.peerDispositions.getValue(extend to peer) }
            val table = buildPublishedOwnerTable(name, extend, related)
            val useSitePackage = accessContext.containingDeclarations.lastOrNull()?.let { declaration ->
                if (declaration is CfirExtend) declaration.getDeclarationPackage() else declaration.symbol.getDeclarationPackage()
            }
            processor(CfirFunctionInstantiationTable(
                table.members.filter { member -> !member.isPrivate &&
                        (member.declaration.cfir.status.visibility == Visibilities.Protected ||
                                session.accessibilityChecker.isCallableVisibleFromPackage(member.declaration, useSitePackage)) },
                table.updates, emptySet(), CfirFunctionMemberTableKind.VISIBLE_EXTEND,
            ))
        }
    }

    /**
     * 对位 extend 的 CheckMembersWithInheritedDecls。peer 接口和 peer 声明分表；
     * 先 instance→visible，再更新当前 owner 的非构造声明，最后 visible→自身 interface。
     */
    private fun buildPublishedOwnerTable(name: Name, owner: CfirExtend, relatedExtends: Set<CfirExtend>): CfirFunctionInstantiationTable {
        val interfaces = inheritedInterfaceTable(name, owner)
        val visibleInterfaces = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
        val visibleFunctions = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
        val ownerPackage = owner.getDeclarationPackage()
        for (peer in relatedExtends.filter { it in scopeInput.instantiationOwners &&
            (it === owner || scopeInput.peerOwners.getValue(owner to it).visible) }) {
            if (peer === owner) continue
            val peerInterfaces = inheritedInterfaceTableForInputs(name, peer,
                scopeInput.peerInheritedInterfaces.getValue(owner to peer)).finish(CfirFunctionMemberTableKind.INTERFACE)
            visibleInterfaces.addTable(CfirFunctionInstantiationTable(
                peerInterfaces.members.map { it.copy(sourceExtend = peer) }, peerInterfaces.updates, emptySet(),
                CfirFunctionMemberTableKind.VISIBLE_EXTEND,
            ))
            for (member in preparedPeerFunctions(name, owner, peer)) {
                if (!member.isPrivate && session.accessibilityChecker.isCallableVisibleFromPackage(member.declaration, ownerPackage)) {
                    visibleFunctions.update(member)
                }
            }
        }
        visibleInterfaces.addTable(visibleFunctions.finish(CfirFunctionMemberTableKind.VISIBLE_EXTEND))
        visibleInterfaces.removeAbstractMembers()

        val nominalScope = scopeInput.nominalScope
        if (nominalScope != null) {
            val nominalTable = checkNotNull(nominalScope as? CfirFunctionInstantiationScope).publishedFunctionMemberTable(name)
            visibleInterfaces.addTable(CfirFunctionInstantiationTable(
                nominalTable.members.filter { !it.isPrivate &&
                        (it.declaration.cfir.status.visibility == Visibilities.Protected ||
                                session.accessibilityChecker.isCallableVisibleFromPackage(it.declaration, ownerPackage)) },
                nominalTable.updates, emptySet(), CfirFunctionMemberTableKind.INSTANCE,
            ))
        }
        for (member in ownerInstantiationFunctions(name, owner)) {
            visibleInterfaces.update(member)
            interfaces.addOwnDeclaration(member.declaration)
        }
        interfaces.addTable(visibleInterfaces.finish(CfirFunctionMemberTableKind.VISIBLE_EXTEND))
        interfaces.removePrivateMembers()
        interfaces.clearPublishedTransientState()
        return interfaces.finish(CfirFunctionMemberTableKind.VISIBLE_EXTEND)
    }

    /** 每条实际接口边单独读取父发布表，接口标志来自 merge 操作而非成员声明的批量推断。 */
    private fun inheritedInterfaceTable(name: Name, owner: CfirExtend): CfirFunctionInstantiationTableBuilder {
        return inheritedInterfaceTableForInputs(name, owner, scopeInput.inheritedInterfaces.getValue(owner))
    }

    private fun inheritedInterfaceTableForInputs(name: Name, owner: CfirExtend,
        inputs: List<InheritedInterfaceInput>): CfirFunctionInstantiationTableBuilder {
        val builder = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
        for (input in inputs) {
            val interfaceType = input.type
            val scope = input.scope
            val table = checkNotNull(scope as? CfirFunctionInstantiationScope).publishedFunctionMemberTable(name)
            if (table.kind != CfirFunctionMemberTableKind.INTERFACE) continue
            val descriptor = CfirInstantiatedSupertypeDescriptor(
                type = interfaceType,
                origin = CfirInstantiatedSupertypeOrigin.Extend(owner, checkNotNull(owner.getDeclarationPackage()), input.sourceTypeRef),
            )
            val provenance = CfirCallableLookupProvenance.inheritedThroughExtend(descriptor)
            builder.addTable(CfirFunctionInstantiationTable(
                table.members.map { it.copy(
                    lookupProvenance = provenance, identity = it.identity.copy(lookupProvenance = provenance),
                ) }, table.updates, emptySet(), CfirFunctionMemberTableKind.INTERFACE,
            ), inheritedInterfaces = true)
        }
        return builder
    }

    private fun preparedPeerFunctions(name: Name, owner: CfirExtend, peer: CfirExtend): List<CfirStoredFunctionMember> {
        val input = scopeInput.peerOwners.getValue(owner to peer)
        val headers = scopeInput.owners.getValue(peer).memberHeaders
        return input.members.mapNotNull { member ->
            val header = headers[member.symbol] ?: return@mapNotNull null
            if (header.name != name || header.kind != CfirInheritanceMemberKind.FUNCTION) return@mapNotNull null
            val mapping = input.memberSubstitutions.getValue(header.declaration)
            header.bindInheritanceMember(this, mapping.receiver,
                createTypeSubstitutorByTypeConstructor(mapping.replacements, session.typeContext, false), peer,
                CfirCallableLookupProvenance.directExtendMember(peer)).function
        }
    }

    /** 普通 extend 和 CJMP 合并成员均先按实际 owner→receiver 映射存储签名与 structTy。 */
    private fun ownerInstantiationFunctions(name: Name, owner: CfirExtend, sourceExtend: CfirExtend? = null): List<CfirStoredFunctionMember> {
        val receiver = scopeInput.receivers.getValue(ReceiverKey(owner, receiverType))
        checkNotNull(receiver.ownerSubstitution) {
            "An instantiation table requires its prepared owner mapping"
        }
        val input = scopeInput.owners.getValue(owner)
        return input.members.mapNotNull { member ->
            val header = input.memberHeaders[member.symbol] ?: return@mapNotNull null
            if (header.kind != CfirInheritanceMemberKind.FUNCTION || header.name != name) return@mapNotNull null
            val mapping = receiver.memberSubstitutions.getValue(header.declaration)
            header.bindInheritanceMember(this, mapping.receiver,
                createTypeSubstitutorByTypeConstructor(mapping.replacements, session.typeContext, false), sourceExtend,
                CfirCallableLookupProvenance.directExtendMember(owner)).function
        }
    }

    private fun selectedInstantiationExtends(): List<ExtendLookupCandidate> {
        val candidates = scopeInput.candidates.filter { candidate ->
            candidate.extend !== excludingExtend && scopeOwner.includesDirectExtend(candidate.extend) &&
                    candidate.extend.isApplicableAtReceiver(candidate.concreteReceiverType)
        }
        return candidates.filter { it.extend in scopeInput.instantiationOwners }
    }

    private fun CfirDeclaration.instantiationAccessContext(): CfirAccessContext = CfirAccessContext(
        useSiteFile = if (this is CfirExtend) getContainingFile() else symbol.getContainingFile(),
        containingDeclarations = listOf(this), receiverType = receiverType,
        lookupOrigin = CfirLookupOrigin.MEMBER, kind = CfirAccessKind.EXTEND,
    )

    override fun functionInstantiationTable(name: Name): CfirFunctionInstantiationTable = instantiationFunctions.getOrPut(name) {
        val builder = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
        builder.copyFrom(publishedFunctionMemberTable(name))
        (scopeOwner as? CfirMemberScopeOwner.ExtendInstantiation)?.extendOwner?.let { owner ->
            ownerInstantiationFunctions(name, owner).filter { it.isPrivate }.forEach { member ->
                builder.addOwnDeclaration(member.declaration)
                builder.update(member.copy(upperBounds = emptyMap()))
            }
        }
        builder.finish(CfirFunctionMemberTableKind.INSTANTIATION_COPY)
    }

    /** extend 文法没有实例构造器，不伪造字段/构造输入。 */
    override fun constructorInstantiationTable(): CfirFunctionInstantiationTable =
        CfirFunctionInstantiationTable(emptyList(), emptyList(), emptySet(), CfirFunctionMemberTableKind.INSTANTIATION_COPY)

    /** direct extend scope 本身不引入继承函数。 */
    override fun processUnmergedInheritedFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    ) = Unit

    /** direct extend scope 本身不建立声明级 override 边。 */
    override fun processDirectOverriddenFunctionsWithProvenance(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirFunctionInheritanceProvenance) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    /**
     * 按名称处理 extend 属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        memberIndex.properties[name]?.forEach(processor)
    }

    /**
     * 按名称处理所有 extend callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    /** 为 extension 候选附加其声明来源，供 owner 作用域和可见性规则共同消费。 */
    override fun processCallablesByNameWithLookupProvenance(
        name: Name,
        processor: (CfirCallableWithLookupProvenance) -> Unit,
    ) {
        processCallablesByName(name) { symbol ->
            val sourceExtend = symbol.unwrapSubstitutionOverrides().getContainingExtend()
            processor(
                CfirCallableWithLookupProvenance(
                    symbol = symbol,
                    provenance = sourceExtend?.let(CfirCallableLookupProvenance::directExtendMember)
                        ?: CfirCallableLookupProvenance.None,
                ),
            )
        }
    }

    /**
     * 按 K2 CDoc 的候选协议，枚举该 receiver target 上的直接成员，暂不做适用性过滤。
     *
     * 返回值保留 extend 来源，调用方必须完成 fresh-variable 类型关系与 use-site 可见性检查；
     * 运行期普通成员查询继续使用 [memberIndex] 的严格候选集合。
     */
    override fun processPotentialDirectExtendCallablesByName(
        name: Name,
        processor: (CfirCallableWithLookupProvenance) -> Unit,
    ) {
        val emitted = linkedSetOf<CfirCallableWithLookupProvenance>()
        val shadowed = if (memberScopeView != CfirCjmpMemberScopeView.RAW && session.isCjmpSpecificCompilationEnabled()) {
            scopeInput.candidates.asSequence().map { it.extend }
                .filter { it.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) }
                .flatMap { specific -> cjmpMemberSnapshot(specific).commonCounterpartsFor(specific).asSequence() }
                .map { it.symbol }.toSet()
        } else {
            emptySet()
        }
        for ((extend, _) in scopeInput.candidates) {
            if (extend === excludingExtend || !scopeOwner.includesDirectExtend(extend) || extend.symbol in shadowed) continue

            for (declaration in scopeInput.owners.getValue(extend).members) {
                if (!scopeOwner.includesPrivateMembersFrom(extend) &&
                    declaration is CfirMemberDeclaration &&
                    declaration.status.visibility == Visibilities.Private
                ) continue
                val symbol = when (declaration) {
                    is CfirNamedFunction -> declaration.symbol.takeIf { declaration.callableNameOrNull() == name }
                    is CfirProperty -> declaration.symbol.takeIf { declaration.name == name }
                    is CfirFieldVariable -> declaration.symbol.takeIf { declaration.name == name }
                    else -> null
                } ?: continue
                val candidate = CfirCallableWithLookupProvenance(
                    symbol = symbol,
                    provenance = CfirCallableLookupProvenance.directExtendMember(extend),
                )
                if (emitted.add(candidate)) processor(candidate)
            }
        }
    }

    /**
     * 按名称处理 extend 变量。
     *
     * 与类声明成员作用域同因：索引已持有变量，只透出 `processCallablesByName`
     * 会让按变量族查询的调用方拿不到 extend 引入的变量成员。
     */
    override fun processVariablesByName(name: Name, processor: (CfirVariableSymbol<*>) -> Unit) {
        memberIndex.variables[name]?.forEach(processor)
    }

    /**
     * extend scope 自身不产生直接覆盖函数链。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    /**
     * extend scope 自身不产生直接覆盖属性链。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction = ProcessorAction.NONE

    /**
     * extend scope 不支持直接跨 session 替换。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: org.cangnova.cangjie.cfir.ScopeSession,
    ): CfirTypeScope? = null

    /**
     * extend 成员索引。
     *
     * @property classifiers classifier 成员索引。
     * @property functions 函数成员索引。
     * @property properties 属性成员索引。
     * @property variables 字段变量成员索引。
     */
    private class MemberIndex(
        /**
         * extend classifier 成员索引。
         */
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        /**
         * extend 函数成员索引。
         */
        val functions: Map<Name, List<CfirNamedFunctionSymbol>>,
        /**
         * extend 属性成员索引。
         */
        val properties: Map<Name, List<CfirPropertySymbol>>,
        /**
         * extend 字段变量成员索引。
         */
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    )

    /**
     * 构建当前 receiver 可见的 extend 成员索引。
     */
    private fun buildIndex(): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val functions = HashMap<Name, MutableList<CfirNamedFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        val extends = scopeInput.candidates
        val hiddenCommonExtends = if (
            memberScopeView != CfirCjmpMemberScopeView.RAW &&
            session.isCjmpSpecificCompilationEnabled()
        ) {
            extends.asSequence()
                .map { it.extend }
                .filter { it.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) }
                .flatMap { specificExtend ->
                    cjmpMemberSnapshot(specificExtend).commonCounterpartsFor(specificExtend)
                        .asSequence().map { it.symbol }
                }
                .toSet()
        } else {
            emptySet()
        }
        for ((extend, concreteReceiverType) in extends) {
            if (extend.symbol in hiddenCommonExtends) {
                continue
            }
            if (!scopeOwner.includesDirectExtend(extend)) continue
            if (extend === excludingExtend) continue
            if (!extend.isApplicableAtReceiver(concreteReceiverType)) continue
            for (declaration in extend.membersForScopeView(concreteReceiverType)) {
                if (!scopeOwner.includesPrivateMembersFrom(extend) &&
                    declaration is CfirMemberDeclaration &&
                    declaration.status.visibility == Visibilities.Private
                ) {
                    continue
                }
                indexDeclaration(
                    declaration = declaration,
                    classifiers = classifiers,
                    functions = functions,
                    properties = properties,
                    variables = variables,
                )
            }
        }
        return MemberIndex(classifiers, functions, properties, variables)
    }

    private fun CfirExtend.membersForScopeView(
        concreteReceiverType: ConeCangJieType,
    ): List<CfirDeclaration> {
        val input = scopeInput.owners.getValue(this)
        check(scopeInput.receivers.containsKey(ReceiverKey(this, concreteReceiverType))) { "Receiver was not part of the prepared scope domain" }
        return input.members
    }

    private fun cjmpMemberSnapshot(extend: CfirExtend): CfirCjmpMemberSnapshot =
        checkNotNull(scopeInput.owners.getValue(extend).snapshot) { "Actualized extend requires its prepared member snapshot" }

    /**
     * extend member scope 必须和 use-site substitution/receiver 检查共享同一适用性规则。
     *
     * 只按目标 ClassId 建索引会把不满足泛型约束的 extend 成员暴露给调用解析；
     * 官方成员访问流程会在候选阶段删除这些目标。
     */
    private fun CfirExtend.isApplicableAtReceiver(concreteReceiverType: ConeCangJieType): Boolean {
        return scopeInput.receivers.getValue(ReceiverKey(this, concreteReceiverType)).applicability != Applicability.INAPPLICABLE
    }


    /**
     * 将单个 extend 成员声明写入索引。
     */
    private fun indexDeclaration(
        declaration: CfirDeclaration,
        classifiers: HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>,
        functions: HashMap<Name, MutableList<CfirNamedFunctionSymbol>>,
        properties: HashMap<Name, MutableList<CfirPropertySymbol>>,
        variables: HashMap<Name, MutableList<CfirVariableSymbol<*>>>,
    ) {
        when (declaration) {
            is CfirClassLikeDeclaration -> {
                val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return
                classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
            }

            is CfirNamedFunction -> {
                val symbol = declaration.symbol ?: return
                val callableName = declaration.callableNameOrNull() ?: return
                functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
            }

            is CfirProperty -> {
                val symbol = declaration.symbol as? CfirPropertySymbol ?: return
                properties.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
            }

            is CfirFieldVariable -> {
                val symbol = declaration.symbol as? CfirVariableSymbol<*> ?: return
                variables.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
            }

            else -> Unit
        }
    }

    /**
     * extend 查询候选。
     *
     * @property extend extend 声明。
     * @property concreteReceiverType 用于匹配该 extend 的具体 receiver 类型。
     */
    internal data class ExtendLookupCandidate(
        /**
         * 当前查询命中的 extend 声明。
         */
        val extend: CfirExtend,
        /**
         * 用于匹配该 extend 的具体 receiver 类型。
         */
        val concreteReceiverType: ConeCangJieType,
    )
}
