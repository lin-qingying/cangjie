package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.cfir.session.services.CfirExtendInterfaceOccurrence
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerDisposition
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerDecision
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerConflictWitness
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerConflictChain
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerConflictRelation
import org.cangnova.cangjie.cfir.session.services.CfirExtendMemberPeerSequenceConflict
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetInterfaceView
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 基于 [CfirExtendIndexStore] 的 extend 规则查询服务实现。
 */
class CfirExtendRuleQueryServiceImpl(
    /** 当前解析会话，提供组合后的类型父图与依赖 extend 元数据。 */
    private val session: CfirSession,
    /**
     * 会话级 extend 索引存储。
     */
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendRuleQueryService {
    /**
     * 返回声明对应的 extend 目标键。
     */
    override fun targetKeyOf(declaration: Any): CfirExtendTargetKey? =
        indexStore.targetKeyOf(declaration)

    /**
     * 返回完整实例化目标模式相同的 extend 声明。
     */
    override fun extendDeclarationsForSameTarget(declaration: Any): List<Any> {
        val targetSemanticKey = indexStore.modelForDeclaration(declaration)?.targetSemanticKey ?: return emptyList()
        return indexStore.modelsForSemanticTarget(targetSemanticKey).map { it.declaration }
    }

    /**
     * 返回同一 nominal target bucket 中的 extend 声明，并保留源码顺序。
     */
    override fun extendDeclarationsForNominalTarget(declaration: Any): List<Any> {
        val targetKey = indexStore.modelForDeclaration(declaration)?.targetKey ?: return emptyList()
        return indexStore.modelsForTarget(targetKey).map { it.declaration }
    }

    /**
     * 返回指定直接接口的重复检查 occurrence 信息。
     */
    override fun duplicateInterfaceOccurrenceOf(
        declaration: Any,
        superTypeIndex: Int,
    ): CfirExtendInterfaceOccurrence? =
        indexStore.duplicateInterfaceOccurrenceOf(declaration, superTypeIndex)

    /**
     * 统一从类型感知父图投影目标已经具备的接口。
     */
    override fun targetAvailableInterfacesOf(
        declaration: Any,
        view: CfirExtendTargetInterfaceView,
    ): List<CfirExtendInheritedInterfaceSemantic> {
        val extend = declaration as? CfirExtend
            ?: error("Target interface query requires a CfirExtend declaration")
        val targetType = requireNotNull(extend.extendedTypeRef.coneTypeOrNull) {
            "Extend target type must be resolved before rule queries: ${extend.symbol}"
        }
        val normalizer = CfirExtendTypeSemanticNormalizer(extend, session, session.typeResolver)
        val result = linkedMapOf<String, CfirExtendInheritedInterfaceSemantic>()
        collectAvailableInterfaces(
            type = targetType,
            currentExtend = extend,
            currentPackage = packageFqNameOf(extend),
            view = view,
            traversalMode = CfirExtendInterfaceTraversalMode.TARGET_ROOT,
            normalizer = normalizer,
            result = result,
            visiting = linkedSetOf(),
        )
        return result.values.toList()
    }

    /**
     * 返回声明对应的目标 classId。
     */
    override fun targetClassIdOf(declaration: Any): ClassId? =
        indexStore.modelForDeclaration(declaration)?.targetClassId

    /**
     * 返回声明所在包名。
     */
    override fun packageFqNameOf(declaration: Any): FqName =
        requireNotNull(indexStore.packageFqNameOf(declaration)) {
            "Extend declaration package is not indexed: $declaration"
        }

    /**
     * 返回声明继承的 interface 语义项。
     */
    override fun inheritedInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaces.orEmpty()

    /**
     * 按目标 classId 查询继承的 interface 语义项。
     */
    override fun inheritedInterfacesForTarget(
        targetClassId: ClassId,
        excludingDeclaration: Any?,
    ): List<CfirExtendInheritedInterfaceSemantic> =
        inheritedInterfacesForTarget(CfirExtendTargetKey.ClassLike(targetClassId), excludingDeclaration)

    /**
     * 按目标键查询继承的 interface 语义项。
     */
    override fun inheritedInterfacesForTarget(
        targetKey: CfirExtendTargetKey,
        excludingDeclaration: Any?,
    ): List<CfirExtendInheritedInterfaceSemantic> {
        return indexStore.modelsForTarget(targetKey)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaces.asSequence() }
            .toList()
    }

    /**
     * 返回声明继承的 interface classId。
     */
    override fun inheritedInterfaceClassIdsOf(declaration: Any): List<ClassId> =
        indexStore.inheritedInterfaceClassIdsOf(declaration)

    /**
     * 按目标 classId 查询继承的 interface classId。
     */
    override fun inheritedInterfaceClassIdsForTarget(targetClassId: ClassId, excludingDeclaration: Any?): List<ClassId> =
        inheritedInterfaceClassIdsForTarget(CfirExtendTargetKey.ClassLike(targetClassId), excludingDeclaration)

    /**
     * 按目标键查询继承的 interface classId。
     */
    override fun inheritedInterfaceClassIdsForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any?): List<ClassId> {
        return indexStore.modelsForTarget(targetKey)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaceClassIds.asSequence() }
            .toList()
    }

    /**
     * 返回声明继承 interface 的闭包 classId 集合。
     */
    override fun inheritedInterfaceClosureClassIdsOf(declaration: Any): Set<ClassId> =
        indexStore.inheritedInterfaceClosureClassIdsOf(declaration)

    /**
     * 判断两个 extend 声明是否处于继承关系。
     */
    override fun areExtendsInInheritRelation(firstDeclaration: Any, secondDeclaration: Any): Boolean =
        indexStore.areExtendsInInheritRelation(firstDeclaration, secondDeclaration)

    /**
     * 判断第一个 extend 的接口检查序列是否位于第二个 extend 之后。
     */
    override fun doesExtendInheritFrom(childDeclaration: Any, parentDeclaration: Any): Boolean =
        indexStore.doesExtendInheritFrom(childDeclaration, parentDeclaration)

    /**
     * 对齐官方 `DeterminingSkipExtendByInheritanceRelationship` 的逐 owner/peer 接口比较。
     *
     * ClassId 闭包只能回答是否存在某种继承关系，无法保留同一直接接口重复出现、多个接口分别
     * 指向父/子方向，以及泛型接口的不同实例。本查询直接消费类型感知父图中的接口实例边。
     */
    override fun extendMemberPeerDecision(
        ownerDeclaration: Any,
        peerDeclaration: Any,
    ): CfirExtendMemberPeerDecision {
        val owner = ownerDeclaration as? CfirExtend
            ?: error("Extend member owner relation requires a CfirExtend owner: $ownerDeclaration")
        val peer = peerDeclaration as? CfirExtend
            ?: error("Extend member owner relation requires a CfirExtend peer: $peerDeclaration")
        if (owner === peer) return CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.INCLUDE)
        val ownerTargetKey = indexStore.targetKeyOf(owner)
            ?: return CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.INCLUDE)
        val peerTargetKey = indexStore.targetKeyOf(peer)
            ?: return CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.EXCLUDE)
        if (ownerTargetKey != peerTargetKey) {
            return CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.EXCLUDE)
        }

        val ownerTargetType = owner.extendedTypeRef.coneTypeOrNull
            ?.fullyExpandedType(session)
            ?: return CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.INCLUDE)
        val descriptors = checkNotNull(session.typeAwareSupertypeProviderOrNull) {
            "Extend member owner ordering requires the type-aware supertype provider"
        }.getDirectSupertypeDescriptors(ownerTargetType)
        val ownerInterfaces = descriptors.directInterfaceTypesFrom(owner)
        val peerInterfaces = descriptors.directInterfaceTypesFrom(peer)
        val typeNormalizer = CfirExtendTypeSemanticNormalizer(owner, session, session.typeResolver)

        fun interfaceSemantic(type: ConeCangJieType): CfirExtendInheritedInterfaceSemantic =
            CfirExtendInheritedInterfaceSemantic(
                classId = type.classIdOrPrimitiveClassId,
                semanticKey = typeNormalizer.semanticKeyOrNull(type),
            )

        fun undecidableDecision(
            conflictChains: List<List<Pair<ConeCangJieType, ConeCangJieType>>>,
        ): CfirExtendMemberPeerDecision = CfirExtendMemberPeerDecision(
            disposition = CfirExtendMemberPeerDisposition.UNDECIDABLE,
            conflictWitness = CfirExtendMemberPeerConflictWitness(
                ownerInterfaces = ownerInterfaces.map(::interfaceSemantic),
                peerInterfaces = peerInterfaces.map(::interfaceSemantic),
                conflictChains = conflictChains.map { chain ->
                    CfirExtendMemberPeerConflictChain(
                        relations = chain.map { (subInterface, superInterface) ->
                            CfirExtendMemberPeerConflictRelation(
                                subInterface = interfaceSemantic(subInterface),
                                superInterface = interfaceSemantic(superInterface),
                            )
                        },
                    )
                },
            ),
        )

        var previousSkipPeer: Boolean? = null
        var previousRelation: Pair<ConeCangJieType, ConeCangJieType>? = null
        for (ownerInterface in ownerInterfaces) {
            var peerProvidesSubtype = false
            var peerProvidesSupertype = false
            var peerSubtypeWitness: ConeCangJieType? = null
            var peerSupertypeWitness: ConeCangJieType? = null
            for (peerInterface in peerInterfaces) {
                if (AbstractTypeChecker.equalTypes(session.typeContext, ownerInterface, peerInterface)) continue
                if (AbstractTypeChecker.isSubtypeOf(session.typeContext, peerInterface, ownerInterface)) {
                    peerProvidesSubtype = true
                    peerSubtypeWitness = peerInterface
                }
                if (AbstractTypeChecker.isSubtypeOf(session.typeContext, ownerInterface, peerInterface)) {
                    peerProvidesSupertype = true
                    peerSupertypeWitness = peerInterface
                }
            }

            if (peerProvidesSubtype && peerProvidesSupertype) {
                return undecidableDecision(
                    listOf(
                        listOf(
                            checkNotNull(peerSubtypeWitness) to ownerInterface,
                            ownerInterface to checkNotNull(peerSupertypeWitness),
                        ),
                    ),
                )
            }
            if (!peerProvidesSubtype && !peerProvidesSupertype) continue

            val skipPeer = peerProvidesSubtype
            val currentRelation = if (skipPeer) {
                checkNotNull(peerSubtypeWitness) to ownerInterface
            } else {
                ownerInterface to checkNotNull(peerSupertypeWitness)
            }
            if (previousSkipPeer != null && previousSkipPeer != skipPeer) {
                return undecidableDecision(
                    listOf(
                        listOf(checkNotNull(previousRelation)),
                        listOf(currentRelation),
                    ),
                )
            }
            previousSkipPeer = skipPeer
            previousRelation = currentRelation
        }

        return if (previousSkipPeer == true) {
            CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.EXCLUDE)
        } else {
            CfirExtendMemberPeerDecision(CfirExtendMemberPeerDisposition.INCLUDE)
        }
    }

    private fun List<CfirInstantiatedSupertypeDescriptor>.directInterfaceTypesFrom(
        extend: CfirExtend,
    ): List<ConeCangJieType> = mapNotNull { descriptor ->
        val origin = descriptor.origin as? CfirInstantiatedSupertypeOrigin.Extend ?: return@mapNotNull null
        if (origin.sourceExtend !== extend || origin.propagationPath.isNotEmpty()) return@mapNotNull null
        val interfaceType = descriptor.type.fullyExpandedType(session)
        val classId = interfaceType.classIdOrPrimitiveClassId ?: return@mapNotNull null
        val declaration = session.typeResolver.resolveClass(classId)
        interfaceType.takeIf { declaration is CfirInterface }
    }

    /**
     * 通过同一 pairwise 实例接口判据查找不可决定的 owner/peer 顺序；该关系同时驱动
     * `EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE` 报告与实例化成员 peer 的纳入决策。
     */
    override fun hasUndecidableExtendCheckSequence(declaration: Any): Boolean =
        undecidableExtendCheckSequenceConflicts(declaration).isNotEmpty()

    override fun undecidableExtendCheckSequenceConflicts(
        declaration: Any,
    ): List<CfirExtendMemberPeerSequenceConflict> {
        val owner = declaration as? CfirExtend ?: return emptyList()
        val targetKey = indexStore.targetKeyOf(owner) ?: return emptyList()
        val ownerPackage = session.extendProvider.getPackageFqName(owner)
        return indexStore.modelsForTarget(targetKey).mapNotNull { model ->
            val peer = model.declaration
            if (peer === owner) return@mapNotNull null
            if (ownerPackage == null || session.extendProvider.getPackageFqName(peer) != ownerPackage) return@mapNotNull null
            extendMemberPeerDecision(owner, peer).conflictWitness?.let { witness ->
                CfirExtendMemberPeerSequenceConflict(peer, witness)
            }
        }
    }

    /**
     * 返回声明继承 interface 的语义 key。
     */
    override fun inheritedInterfaceSemanticKeysOf(declaration: Any): List<String> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaceSemanticKeys.orEmpty()

    /**
     * 按目标 classId 查询继承 interface 的语义 key。
     */
    override fun inheritedInterfaceSemanticKeysForTarget(targetClassId: ClassId, excludingDeclaration: Any?): List<String> =
        inheritedInterfaceSemanticKeysForTarget(CfirExtendTargetKey.ClassLike(targetClassId), excludingDeclaration)

    /**
     * 按目标键查询继承 interface 的语义 key。
     */
    override fun inheritedInterfaceSemanticKeysForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any?): List<String> {
        return indexStore.modelsForTarget(targetKey)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaceSemanticKeys.asSequence() }
            .toList()
    }

    /**
     * 返回 interface 中默认独立成员名称。
     */
    override fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name> =
        indexStore.defaultIndependentMembersOfInterface(interfaceClassId)

    /**
     * 返回 extend 继承接口连同传递父接口的语义列表。
     */
    override fun inheritedInterfaceClosureOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaceClosure.orEmpty()

    /**
     * 判断声明是否是目标 classId 上的第一个 extend。
     */
    override fun isFirstExtendForTarget(declaration: Any, targetClassId: ClassId): Boolean =
        indexStore.isFirstExtendForTarget(declaration, targetClassId)

    /**
     * 判断声明是否是目标键上的第一个 extend。
     */
    override fun isFirstExtendForTarget(declaration: Any, targetKey: CfirExtendTargetKey): Boolean =
        indexStore.isFirstExtendForTarget(declaration, targetKey)

    /** 递归收集父图中的接口节点；origin 与递归深度只在共享 owner 内解释。 */
    private fun collectAvailableInterfaces(
        type: ConeCangJieType,
        currentExtend: CfirExtend,
        currentPackage: FqName,
        view: CfirExtendTargetInterfaceView,
        traversalMode: CfirExtendInterfaceTraversalMode,
        normalizer: CfirExtendTypeSemanticNormalizer,
        result: MutableMap<String, CfirExtendInheritedInterfaceSemantic>,
        visiting: MutableSet<String>,
    ) {
        val visitingKey = "${traversalMode.name}:${normalizer.semanticKeyOrNull(type)}"
        if (!visiting.add(visitingKey)) return

        val descriptors = session.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypeDescriptors(type)
            .orEmpty()
        for (descriptor in descriptors) {
            val decision = descriptor.traversalDecision(
                currentExtend = currentExtend,
                currentPackage = currentPackage,
                view = view,
                traversalMode = traversalMode,
            )
            if (decision == CfirExtendInterfaceEdgeDecision.SKIP) continue

            val semanticSupertype = descriptor.type.fullyExpandedType(session)
            semanticSupertype.addInterfaceSemanticIfNeeded(normalizer, result)
            val nextTraversalMode = decision.nextTraversalMode ?: continue
            collectAvailableInterfaces(
                type = semanticSupertype,
                currentExtend = currentExtend,
                currentPackage = currentPackage,
                view = view,
                traversalMode = nextTraversalMode,
                normalizer = normalizer,
                result = result,
                visiting = visiting,
            )
        }
        visiting.remove(visitingKey)
    }

    /**
     * 把父边投影为明确的收集决策。
     *
     * 官方 duplicate 与 orphan 都使用父图，但两者的边界不同：duplicate 对同目标
     * extend 只比较直接 `inheritedTypes`，orphan 则需要展开其他包 extend 接口的
     * nominal 父接口闭包。因此这里不能再用单个 Boolean 表示“可见即递归”。
     */
    private fun CfirInstantiatedSupertypeDescriptor.traversalDecision(
        currentExtend: CfirExtend,
        currentPackage: FqName,
        view: CfirExtendTargetInterfaceView,
        traversalMode: CfirExtendInterfaceTraversalMode,
    ): CfirExtendInterfaceEdgeDecision = when (val edgeOrigin = origin) {
        is CfirInstantiatedSupertypeOrigin.Declared -> when (traversalMode) {
            CfirExtendInterfaceTraversalMode.TARGET_ROOT -> when (view) {
                CfirExtendTargetInterfaceView.DUPLICATE_BASELINE ->
                    CfirExtendInterfaceEdgeDecision.INCLUDE_AND_RECURSE_DUPLICATE_NOMINAL

                CfirExtendTargetInterfaceView.ORPHAN_BASELINE ->
                    CfirExtendInterfaceEdgeDecision.INCLUDE_AND_RECURSE_DECLARED_ONLY
            }

            CfirExtendInterfaceTraversalMode.DUPLICATE_NOMINAL_CLOSURE ->
                CfirExtendInterfaceEdgeDecision.INCLUDE_AND_RECURSE_DUPLICATE_NOMINAL

            CfirExtendInterfaceTraversalMode.DECLARED_ONLY_CLOSURE ->
                CfirExtendInterfaceEdgeDecision.INCLUDE_AND_RECURSE_DECLARED_ONLY
        }

        /*
         * Cfir 的 ImplicitObject 边表达类型系统的 Object/Any 通用关系；官方
         * CheckExtendInterfaces/CheckExtendOrphanRule 都不把该通用关系当作声明接口输入。
         */
        is CfirInstantiatedSupertypeOrigin.ImplicitObject -> CfirExtendInterfaceEdgeDecision.SKIP

        is CfirInstantiatedSupertypeOrigin.Extend -> {
            if (edgeOrigin.sourceExtend === currentExtend) {
                CfirExtendInterfaceEdgeDecision.SKIP
            } else {
                when (view) {
                    CfirExtendTargetInterfaceView.DUPLICATE_BASELINE -> when (traversalMode) {
                        CfirExtendInterfaceTraversalMode.TARGET_ROOT -> {
                            if (edgeOrigin.propagationPath.isEmpty()) {
                                CfirExtendInterfaceEdgeDecision.INCLUDE_ONLY
                            } else {
                                /* 传播边由 declared superclass 路径回到真实 owner 处收集。 */
                                CfirExtendInterfaceEdgeDecision.SKIP
                            }
                        }

                        CfirExtendInterfaceTraversalMode.DUPLICATE_NOMINAL_CLOSURE -> {
                            if (edgeOrigin.propagationPath.isEmpty()) {
                                CfirExtendInterfaceEdgeDecision.INCLUDE_AND_RECURSE_DUPLICATE_NOMINAL
                            } else {
                                CfirExtendInterfaceEdgeDecision.SKIP
                            }
                        }

                        CfirExtendInterfaceTraversalMode.DECLARED_ONLY_CLOSURE ->
                            CfirExtendInterfaceEdgeDecision.SKIP
                    }

                    CfirExtendTargetInterfaceView.ORPHAN_BASELINE -> when (traversalMode) {
                        CfirExtendInterfaceTraversalMode.TARGET_ROOT -> {
                            if (edgeOrigin.declarationPackage != currentPackage) {
                                CfirExtendInterfaceEdgeDecision.INCLUDE_AND_RECURSE_DECLARED_ONLY
                            } else {
                                CfirExtendInterfaceEdgeDecision.SKIP
                            }
                        }

                        CfirExtendInterfaceTraversalMode.DUPLICATE_NOMINAL_CLOSURE,
                        CfirExtendInterfaceTraversalMode.DECLARED_ONLY_CLOSURE,
                        -> CfirExtendInterfaceEdgeDecision.SKIP
                    }
                }
            }
        }
    }

    /** 将接口父节点投影到 extend 规则的稳定语义表示。 */
    private fun ConeCangJieType.addInterfaceSemanticIfNeeded(
        normalizer: CfirExtendTypeSemanticNormalizer,
        result: MutableMap<String, CfirExtendInheritedInterfaceSemantic>,
    ) {
        val classId = classIdOrPrimitiveClassId ?: return
        val hasInterfaceShape = (this as? ConeClassLikeType)?.isInterface == true
        if (!hasInterfaceShape) {
            val declaration = (session.symbolProvider.getClassLikeSymbolByClassId(classId) as? CfirClassLikeSymbol<*>)
                ?.cfir
            if (declaration !is CfirInterface) return
        }
        val semanticKey = normalizer.semanticKeyOrNull(this)
        result.putIfAbsent(
            semanticKey,
            CfirExtendInheritedInterfaceSemantic(classId, semanticKey),
        )
    }

    /** 父图递归所处的官方语义投影。 */
    private enum class CfirExtendInterfaceTraversalMode {
        /** 当前 extend 的目标类型根节点。 */
        TARGET_ROOT,

        /** duplicate 的 nominal 父图闭包，包含该 nominal 类型自身的 extend 接口。 */
        DUPLICATE_NOMINAL_CLOSURE,

        /** 只沿声明父边展开的 nominal 接口闭包。 */
        DECLARED_ONLY_CLOSURE,
    }

    /** 单条父边的收集与递归决策。 */
    private enum class CfirExtendInterfaceEdgeDecision(
        val nextTraversalMode: CfirExtendInterfaceTraversalMode?,
    ) {
        /** 当前规则不消费该边。 */
        SKIP(null),

        /** 只收集该边指向的直接接口。 */
        INCLUDE_ONLY(null),

        /** 收集该边，并按 duplicate nominal 语义继续展开。 */
        INCLUDE_AND_RECURSE_DUPLICATE_NOMINAL(CfirExtendInterfaceTraversalMode.DUPLICATE_NOMINAL_CLOSURE),

        /** 收集该边，并仅沿 nominal 声明父边继续展开。 */
        INCLUDE_AND_RECURSE_DECLARED_ONLY(CfirExtendInterfaceTraversalMode.DECLARED_ONLY_CLOSURE),
    }
}
