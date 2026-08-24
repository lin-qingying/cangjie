package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCreateFreshTypeVariableSubstitutorStage
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.types.createTypeSubstitutorByTypeConstructor
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.cangnova.cangjie.resolve.calls.results.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker
import org.cangnova.cangjie.type.model.TypeSubstitutorMarker
import org.cangnova.cangjie.type.model.TypeSystemInferenceExtensionContext

/**
 * 参与重载消歧的候选平铺签名。
 */
typealias CandidateSignature = FlatSignature<Candidate>

/**
 * CFIR 重载冲突解析器。
 *
 * 该实现保持 Kotlin FIR `ConeOverloadConflictResolver` 的控制流骨架，同时接入仓颉特有的
 * 变参、extend、quest fallback、理想数值类型和公共类型变元比较规则。
 */
class ConeOverloadConflictResolver(
    /** 通用类型特异性比较器。 */
    private val specificityComparator: TypeSpecificityComparator,
    /** 推断组件集合，用于构造临时约束系统和访问 session 类型上下文。 */
    private val inferenceComponents: InferenceComponents,
    /** body resolve 组件，保留为与 resolver 构造协议一致的依赖。 */
    @Suppress("unused") private val transformerComponents: BodyResolveComponents,
) : ConeCallConflictResolver() {

    /**
     * 从候选集合中选择最大特异候选集合。
     */
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate> = chooseMaximallySpecificCandidates(
        candidates,
        // The local CFIR model does not yet expose a dedicated callable-reference call-site node.
        // Kotlin FIR disables generic discrimination for callable references, so we derive the same
        // distinction from the candidate payload that is only initialized for callable references.
        discriminateGenerics = candidates.first().resultingTypeForCallableReference == null,
    )

    /**
     * Partial mirror of Kotlin FIR's `ConeOverloadConflictResolver.chooseMaximallySpecificCandidates`.
     *
     * The framework shape is intentionally kept aligned with upstream FIR. The only dropped pieces are
     * branches that the current Cangjie front-end does not model yet:
     * - context receivers
     * - property-for-invoke common receiver candidates
     * - callable-reference postponed atoms
     * - low-priority SAM diagnostics stage metadata
     */
    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
    ): Set<Candidate> {
        if (candidates.size == 1) return candidates

        val fixedCandidates = chooseCandidatesWithMostSpecificInvokeReceiver(candidates)
        val candidatesWithoutOverrides = filterOverrides(fixedCandidates)
        val candidatesWithoutCrossOwnerDuplicates =
            collapseCrossOwnerSameSignatureMembers(candidatesWithoutOverrides)

        return chooseMaximallySpecificCandidates(
            candidatesWithoutCrossOwnerDuplicates,
            DiscriminationFlags(
                lowPrioritySAMs = true,
                adaptationsInPostponedAtoms = true,
                generics = discriminateGenerics,
                SAMs = true,
                suspendConversions = true,
                byUnwrappedSmartCastOrigin = true,
            )
        )
    }

    /**
     * 对齐官方 `ResolveOverload` 的两条跨 owner 成员消重规则
     * （`TypeCheckCall.cpp:754-766` 与 `TypeCheckCall.cpp:792, 321-333`）：
     *
     * 1. 参数类型完全一致的普通类实现成员支配接口声明（或抽象）成员——
     *    官方注释明确"interface allows multiple inheritance...resolve shadow case"，
     *    因此 `T <: C & I` 且 `foo` 在 C 与 I 各有一份时，调用点选择 C.foo，不报歧义；
     * 2. 不同接口 owner 下替换后签名完全一致的候选只保留一个——
     *    `T <: I<U> & I2<U>` 时同名同签名的接口默认成员不会构成调用歧义。
     *
     * 规则 1 的"具体实现"侧包含 class/struct 成员与 extend 提供的成员（fixture 09/10 的
     * extend C { foo } vs I.foo 与官方行为一致）；class/struct 成员与 extend 成员之间的
     * 配对不受本规则影响——官方保留两者并在声明侧报告 EXTEND_FUNCTION_CANNOT_OVERRIDDEN，
     * 调用侧仍允许真实歧义。顶层函数与同 owner 重载也走正常 most-specific 比较。
     *
     * 两条规则只作用于**类型参数接收者**（`T <: ...` 的交叉 upper bound 成员查找）。
     * 具体类实例接收者上经 extend/继承聚合出的同签名接口默认成员仍保留歧义——官方对
     * `C().foo()`（C 经两个 extend 分别实现 I3/I4 默认成员）报告 ambiguous match。
     */
    private fun collapseCrossOwnerSameSignatureMembers(candidateSet: Set<Candidate>): Set<Candidate> {
        if (candidateSet.size <= 1) return candidateSet

        // 仅类型参数接收者参与官方 bound-merge 消重；其余接收者保持原语义。
        val receiverIsTypeParameter = candidateSet.all { candidate ->
            candidate.callInfo.explicitReceiver?.coneTypeOrNull is ConeTypeParameterType
        }
        if (!receiverIsTypeParameter) return candidateSet

        val memberShapes = linkedMapOf<Candidate, MemberOwnerShape>()
        for (candidate in candidateSet) {
            val shape = candidate.memberOwnerShapeForConflictCollapse() ?: continue
            memberShapes[candidate] = shape
        }
        // 非 member 函数候选存在时不做该消重，保持原有语义。
        if (memberShapes.isEmpty() || memberShapes.size != candidateSet.size) return candidateSet

        val entries = memberShapes.entries.toList()
        val dominated = hashSetOf<Candidate>()
        val sameSignatureInterfaceSeen = hashSetOf<Candidate>()

        for (i in entries.indices) {
            val (candidateI, shapeI) = entries[i]
            if (candidateI in dominated || candidateI in sameSignatureInterfaceSeen) continue
            for (j in i + 1 until entries.size) {
                val (candidateJ, shapeJ) = entries[j]
                if (candidateJ in dominated || candidateJ in sameSignatureInterfaceSeen) continue

                // 两条规则都只针对参数列表完全一致的成员对；参数不同的候选交给
                // 正常 most-specific 比较。
                if (!shapeI.hasIdenticalParameterTypes(shapeJ)) continue

                // 规则 1：class/struct/extend 提供的具体实现支配接口声明或抽象成员。
                if (shapeI.isConcreteImplementationMember && shapeJ.isInterfaceOrAbstractDeclaration) {
                    dominated += candidateJ
                    continue
                }
                if (shapeJ.isConcreteImplementationMember && shapeI.isInterfaceOrAbstractDeclaration) {
                    dominated += candidateI
                    break
                }

                // 规则 2：不同接口 owner 的完全一致签名只保留先出现的一个。
                if (shapeI.ownerKind == MemberOwnerKind.INTERFACE && shapeJ.ownerKind == MemberOwnerKind.INTERFACE &&
                    shapeI.ownerClassId != shapeJ.ownerClassId &&
                    shapeI.hasIdenticalFullSignature(shapeJ)
                ) {
                    sameSignatureInterfaceSeen += candidateJ
                }
            }
        }

        if (dominated.isEmpty() && sameSignatureInterfaceSeen.isEmpty()) return candidateSet
        val result = candidateSet.filterTo(linkedSetOf()) { candidate ->
            candidate !in dominated && candidate !in sameSignatureInterfaceSeen
        }
        require(result.isNotEmpty()) { "All candidates filtered out from $candidateSet" }
        return result
    }

    /**
     * 跨 owner 消重使用的成员形状：owner 分类 + 替换后参数/返回类型。
     */
    private data class MemberOwnerShape(
        /** owner ClassId；成员必有。 */
        val ownerClassId: ClassId,
        /** owner 分类。 */
        val ownerKind: MemberOwnerKind,
        /** 函数自身是否 abstract。 */
        val isAbstractFunction: Boolean,
        /** 替换后参数类型列表。 */
        val parameterTypes: List<TypeWithConversion>,
        /** 替换后返回类型。 */
        val returnType: ConeCangJieType?,
    ) {
        /** 是否为 class/struct 成员或 extend 成员中的具体实现。 */
        val isConcreteImplementationMember: Boolean
            get() = !isAbstractFunction &&
                (ownerKind == MemberOwnerKind.CLASS_LIKE || ownerKind == MemberOwnerKind.EXTEND)

        /** 是否可被实现成员覆盖：接口声明成员或抽象成员。 */
        val isInterfaceOrAbstractDeclaration: Boolean
            get() = ownerKind == MemberOwnerKind.INTERFACE || isAbstractFunction
    }

    /** 冲突消重使用的成员 owner 分类。 */
    private enum class MemberOwnerKind {
        /** interface 声明成员。 */
        INTERFACE,

        /** class/struct/enum 成员。 */
        CLASS_LIKE,

        /** extend 提供的成员。 */
        EXTEND,
    }

    /** 参数类型逐一 equalTypes 判定（不含返回类型）。 */
    private fun MemberOwnerShape.hasIdenticalParameterTypes(other: MemberOwnerShape): Boolean =
        parameterTypes.size == other.parameterTypes.size &&
            parameterTypes.zip(other.parameterTypes).all { (left, right) ->
                val leftType = left.resultType ?: return@all false
                val rightType = right.resultType ?: return@all false
                AbstractTypeChecker.equalTypes(inferenceComponents.session.typeContext, leftType, rightType)
            }

    /** 参数与返回类型都一致的全签名判定。 */
    private fun MemberOwnerShape.hasIdenticalFullSignature(other: MemberOwnerShape): Boolean =
        hasIdenticalParameterTypes(other) &&
            returnType != null && other.returnType != null &&
            AbstractTypeChecker.equalTypes(
                inferenceComponents.session.typeContext,
                returnType,
                other.returnType,
            )

    /**
     * 提取候选的跨 owner 消重形状；仅覆盖带 class-like/extend owner 的命名函数成员。
     */
    private fun Candidate.memberOwnerShapeForConflictCollapse(): MemberOwnerShape? {
        val callableSymbol = symbol as? CfirNamedFunctionSymbol ?: return null
        val declaration = callableSymbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return null
        val containingExtend = callableSymbol.getContainingExtend()
        if (containingExtend != null) {
            // extend 成员：官方把 extend 视为对扩展类型的具体实现参与成员遮蔽判定。
            val extendedClassId = containingExtend.extendedTypeRef?.coneTypeOrNull
                ?.fullyExpandedType()?.classIdOrPrimitiveClassId
                ?: return null
            return MemberOwnerShape(
                ownerClassId = extendedClassId,
                ownerKind = MemberOwnerKind.EXTEND,
                isAbstractFunction = declaration.status.isAbstract,
                parameterTypes = computeSignatureTypes(this, declaration),
                returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType,
            )
        }
        val ownerClassId = callableSymbol.callableId.classId ?: return null
        val ownerSymbol = inferenceComponents.session.symbolProvider
            .getClassLikeSymbolByClassId(ownerClassId) ?: return null
        val ownerKind = when (ownerSymbol.cfir) {
            is CfirInterface -> MemberOwnerKind.INTERFACE
            else -> MemberOwnerKind.CLASS_LIKE
        }
        return MemberOwnerShape(
            ownerClassId = ownerClassId,
            ownerKind = ownerKind,
            isAbstractFunction = declaration.status.isAbstract,
            parameterTypes = computeSignatureTypes(this, declaration),
            returnType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType,
        )
    }

    /**
     * 移除被同一 scope 中更具体 override 候选覆盖的候选。
     */
    private fun filterOverrides(candidateSet: Set<Candidate>): Set<Candidate> {
        if (candidateSet.size <= 1) return candidateSet

        val result = linkedSetOf<Candidate>()

        outerLoop@ for (candidate in candidateSet) {
            val iterator = result.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (candidate.overrides(other)) {
                    iterator.remove()
                } else if (other.overrides(candidate)) {
                    continue@outerLoop
                }
            }

            result += candidate
        }

        require(result.isNotEmpty()) { "All candidates filtered out from $candidateSet" }
        return result
    }

    /**
     * 判断当前候选是否 override 另一个候选。
     */
    private fun Candidate.overrides(other: Candidate): Boolean {
        val candidateSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        val otherSymbol = other.symbol as? CfirCallableSymbol<*> ?: return false

        val otherOriginal = otherSymbol.unwrapSubstitutionOverrides()
        if (otherOriginal.isExtendMemberForConflictFiltering()) return false
        if (candidateSymbol.unwrapSubstitutionOverrides() == otherOriginal) return true

        val scope = originScope as? CfirTypeScope ?: return false

        return when (candidateSymbol) {
            // 仓颉的 override 决议只涉及 named function：
            // constructor / enum constructor / init / property accessor 不参与普通重写判定，
            // 按 CfirNamedFunctionSymbol 窄化即可满足 CfirTypeScope API。
            is CfirNamedFunctionSymbol -> overrides(
                MemberWithBaseScope(candidateSymbol, scope),
                otherOriginal,
            ) { baseScope, symbol, processor ->
                baseScope.processDirectOverriddenFunctionsWithBaseScope(symbol, processor)
            }

            is CfirPropertySymbol -> overrides(
                MemberWithBaseScope(candidateSymbol, scope),
                otherOriginal,
            ) { baseScope, symbol, processor ->
                baseScope.processDirectOverriddenPropertiesWithBaseScope(symbol, processor)
            }

            else -> false
        }
    }

    /**
     * 父类型 extend 成员不能在 overload conflict 阶段被普通子类成员按 override 关系删除。
     *
     * 官方会保留两者并报告调用歧义；声明侧另由继承检查报告
     * `EXTEND_FUNCTION_CANNOT_OVERRIDDEN`。
     */
    private fun CfirCallableSymbol<*>.isExtendMemberForConflictFiltering(): Boolean =
        getContainingExtend() != null

    /**
     * 选择拥有最具体 invoke receiver 的候选。
     */
    private fun chooseCandidatesWithMostSpecificInvokeReceiver(candidates: Set<Candidate>): Set<Candidate> {
        // Kotlin FIR has a dedicated `candidateForCommonInvokeReceiver` slot for property+invoke groups.
        // The current CFIR call model does not carry that structure yet, so this hook is intentionally
        // kept as an identity step to preserve upstream control flow.
        return candidates
    }

    /**
     * 重载消歧各个歧视步骤是否仍启用的标志集合。
     */
    private data class DiscriminationFlags(
        /** 是否使用低优先级 SAM 过滤。 */
        val lowPrioritySAMs: Boolean,
        /** 是否使用 postponed atom adaptation 过滤。 */
        val adaptationsInPostponedAtoms: Boolean,
        /** 是否启用泛型候选歧视。 */
        val generics: Boolean,
        /** 是否启用 SAM 转换过滤。 */
        val SAMs: Boolean,
        /** 是否启用函数种类转换过滤。 */
        val suspendConversions: Boolean,
        /** 是否按 smart-cast 原始类型过滤。 */
        val byUnwrappedSmartCastOrigin: Boolean,
    )

    /**
     * 按歧视标志逐层收敛候选集合。
     */
    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminationFlags: DiscriminationFlags,
    ): Set<Candidate> {
        if (discriminationFlags.lowPrioritySAMs) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.shouldHaveLowPriorityDueToSAM() },
                { discriminationFlags.copy(lowPrioritySAMs = false) },
            )?.let { return it }
        }

        if (discriminationFlags.adaptationsInPostponedAtoms) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.hasPostponedAtomWithAdaptation() },
                { discriminationFlags.copy(adaptationsInPostponedAtoms = false) },
            )?.let { return it }
        }

        findMaximallySpecificCall(candidates, discriminateGenerics = false)?.let { return setOf(it) }

        if (discriminationFlags.generics) {
            findMaximallySpecificCall(candidates, discriminateGenerics = true)?.let { return setOf(it) }
        }

        if (discriminationFlags.SAMs) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.usesSamConversionOrSamConstructor },
                { discriminationFlags.copy(SAMs = false) },
            )?.let { return it }
        }

        if (discriminationFlags.suspendConversions) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.usesFunctionKindConversion },
                { discriminationFlags.copy(suspendConversions = false) },
            )?.let { return it }
        }

        if (discriminationFlags.byUnwrappedSmartCastOrigin) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.isFromOriginalTypeInPresenceOfSmartCast },
                { discriminationFlags.copy(byUnwrappedSmartCastOrigin = false) },
            )?.let { return it }
        }

        val filteredSamCandidates = candidates.filterTo(linkedSetOf()) { it.usesSamConversionOrSamConstructor }
        if (filteredSamCandidates.isNotEmpty()) {
            findMaximallySpecificCall(
                candidates,
                discriminateGenerics = false,
                useOriginalSamTypes = true,
            )?.let { return setOf(it) }
        }

        chooseByCangjieSpecificity(candidates)?.let { return it }

        return candidates
    }

    /**
     * 根据单个歧视标志过滤候选；过滤后仍有多候选时关闭当前标志递归比较。
     */
    private inline fun filterCandidatesByDiscriminationFlag(
        candidates: Set<Candidate>,
        filter: (Candidate) -> Boolean,
        newFlags: () -> DiscriminationFlags,
    ): Set<Candidate>? {
        val filtered = candidates.filterTo(linkedSetOf()) { filter(it) }
        return when (filtered.size) {
            1 -> filtered
            0, candidates.size -> null
            else -> chooseMaximallySpecificCandidates(filtered, newFlags())
        }
    }

    /**
     * 应用仓颉特有的候选优先级规则。
     */
    private fun chooseByCangjieSpecificity(candidates: Set<Candidate>): Set<Candidate>? {
        // 官方 ChkCallExpr 只有在普通匹配没有结果时才进入变参解糖；
        // 若同一候选集合中已有普通调用候选成功，实际变参候选不能继续参与冲突。
        preferCandidates(candidates) { !it.usesCangjieVariadicCall }?.let { return it }
        preferCandidates(candidates) { !it.usedExtendParticipation }?.let { return it }
        preferCandidates(candidates) { !it.usedQuestFallback }?.let { return it }
        preferCandidates(candidates) { !it.usedIdealNumericCompatibility }?.let { return it }
        return null
    }

    /**
     * 保留满足偏好条件的候选。
     */
    private inline fun preferCandidates(
        candidates: Set<Candidate>,
        predicate: (Candidate) -> Boolean,
    ): Set<Candidate>? {
        val preferred = candidates.filterTo(linkedSetOf()) { predicate(it) }
        return when (preferred.size) {
            0, candidates.size -> null
            else -> preferred
        }
    }

    /**
     * 判断候选是否应因 SAM 而降低优先级。
     */
    private fun Candidate.shouldHaveLowPriorityDueToSAM(): Boolean {
        // Kotlin FIR threads this signal from dedicated resolution stages.
        // The current Cangjie pipeline keeps SAM conversion data but not the low-priority stage marker.
        return false
    }

    /**
     * 判断候选的 postponed atom 中是否发生 adaptation。
     */
    private fun Candidate.hasPostponedAtomWithAdaptation(): Boolean {
        // Callable-reference postponed atoms have not been introduced in the local CFIR atom hierarchy yet.
        return false
    }

    /**
     * 在候选集合中查找唯一最大特异调用。
     */
    private fun findMaximallySpecificCall(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false,
    ): Candidate? {
        if (candidates.size <= 1) return candidates.singleOrNull()

        val candidateSignatures = candidates.map(::createFlatSignature)
        val bestCandidatesByParameterTypes = candidateSignatures.filter { signature ->
            candidateSignatures.all { other ->
                signature === other || isEquallyOrMoreSpecificCallWithArgumentMapping(
                    signature,
                    other,
                    discriminateGenerics,
                    useOriginalSamTypes,
                )
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith()?.origin
    }

    /**
     * 比较两个候选签名在实参映射下是否前者不弱于后者。
     */
    private fun isEquallyOrMoreSpecificCallWithArgumentMapping(
        call1: CandidateSignature,
        call2: CandidateSignature,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false,
    ): Boolean {
        return compareCallsByUsedArguments(call1, call2, discriminateGenerics, useOriginalSamTypes)
    }

    /**
     * 返回列表中唯一的最大特异签名。
     */
    private fun List<CandidateSignature>.exactMaxWith(): CandidateSignature? {
        var result: CandidateSignature? = null
        for (candidate in this) {
            if (result == null || checkExpectAndEquallyOrMoreSpecificShape(candidate, result)) {
                result = candidate
            }
        }

        if (result == null) return null
        if (any { it != result && checkExpectAndEquallyOrMoreSpecificShape(it, result) }) {
            return null
        }

        return result
    }

    /**
     * 比较两个签名的 expect/shape 层面优先级。
     */
    private fun checkExpectAndEquallyOrMoreSpecificShape(
        call1: CandidateSignature,
        call2: CandidateSignature,
    ): Boolean {
        val hasVarargs1 = call1.hasVarargs
        val hasVarargs2 = call2.hasVarargs
        if (hasVarargs1 && !hasVarargs2) return false
        if (!hasVarargs1 && hasVarargs2) return true

        return true
    }

    /**
     * 使用参数类型、泛型歧视和 SAM 原始类型规则比较两个签名。
     */
    private fun compareCallsByUsedArguments(
        call1: CandidateSignature,
        call2: CandidateSignature,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean,
    ): Boolean {
        if (discriminateGenerics) {
            val isGeneric1 = call1.isGeneric
            val isGeneric2 = call2.isGeneric

            when {
                // Kotlin 在第二轮比较中让非泛型候选直接赢过泛型候选。
                // 仓颉官方 CompareFuncCandidates 仍会拿非泛型候选的参数类型
                // 去推导泛型候选的类型参数；推导失败时必须保留 ambiguous。
                !isGeneric1 && isGeneric2 -> {}
                isGeneric1 -> return false
            }
        }

        val isEquallyOrMoreSpecific = createEmptyConstraintSystem().isSignatureEquallyOrMoreSpecific(
            call1,
            call2,
            SpecificityComparisonWithNumerics,
            specificityComparator,
            useOriginalSamTypes,
        )
        if (!isEquallyOrMoreSpecific) return false

        return satisfiesCangjieCommonTypeVariableRule(call1, call2)
    }

    /**
     * 对齐官方 Cangjie `TypeCheckCall.cpp::CompareFuncCandidates` 中
     * `LocalTypeArgumentSynthesis` 对公共类型变元的约束。
     *
     * Kotlin 的 `FlatSignature` 只要求比较约束系统没有 contradiction；
     * 仓颉在候选 A 的参数类型被拿去推导候选 B 的泛型参数时，还要求每个
     * 在 B 的参数列表中重复出现的类型参数能从 A 的对应位置选出一个一致替代类型。
     * 该规则决定 `g<X>(X, () -> X)` 与 `g<Y>(Y, () -> A)` 这类调用不能因为
     * 调用实参已经把某个候选推窄，就提前丢掉另一个官方认为仍可竞争的候选。
     */
    private fun satisfiesCangjieCommonTypeVariableRule(
        specific: CandidateSignature,
        general: CandidateSignature,
    ): Boolean {
        val trackedParameters = general.typeParameters
            .filterIsInstance<ConeTypeParameterLookupTag>()
            .toSet()
        if (trackedParameters.isEmpty()) return true

        val occurrences = linkedMapOf<ConeTypeParameterLookupTag, MutableList<CommonTypeVariableOccurrence>>()
        for (index in specific.valueParameterTypes.indices) {
            val specificType = specific.valueParameterTypes[index]?.resultType as? ConeCangJieType ?: continue
            val generalType = general.valueParameterTypes.getOrNull(index)?.resultType as? ConeCangJieType ?: continue
            if (!satisfiesCangjieContextTypeVariableRule(specificType, generalType, trackedParameters)) return false
            collectCommonTypeVariableOccurrences(
                specificType = specificType,
                generalType = generalType,
                positionVariance = TypePositionVariance.COVARIANT,
                trackedParameters = trackedParameters,
                occurrences = occurrences,
            )
        }

        return occurrences.values.all { occurrenceList ->
            occurrenceList.size <= 1 || hasConsistentCommonTypeVariableTarget(occurrenceList)
        }
    }

    /**
     * 类型参数出现位置的方差。
     */
    private enum class TypePositionVariance {
        /** 协变位置。 */
        COVARIANT,
        /** 逆变位置。 */
        CONTRAVARIANT,
        /** 不变位置。 */
        INVARIANT;

        /**
         * 进入函数参数位置时翻转方差。
         */
        fun flip(): TypePositionVariance = when (this) {
            COVARIANT -> CONTRAVARIANT
            CONTRAVARIANT -> COVARIANT
            INVARIANT -> INVARIANT
        }

        /**
         * 强制转换为不变位置。
         */
        fun invariant(): TypePositionVariance = INVARIANT
    }

    /**
     * 公共类型变元在 specific 候选参数类型中的一次出现。
     */
    private data class CommonTypeVariableOccurrence(
        /** specific 候选对应位置提供的类型。 */
        val specificType: ConeCangJieType,
        /** 该出现位置的方差。 */
        val variance: TypePositionVariance,
    )

    /**
     * 递归收集 general 候选中被跟踪类型参数的出现位置。
     */
    private fun collectCommonTypeVariableOccurrences(
        specificType: ConeCangJieType,
        generalType: ConeCangJieType,
        positionVariance: TypePositionVariance,
        trackedParameters: Set<ConeTypeParameterLookupTag>,
        occurrences: MutableMap<ConeTypeParameterLookupTag, MutableList<CommonTypeVariableOccurrence>>,
    ) {
        val directTypeParameter = generalType.typeParameterLookupTagOrNull()
        if (directTypeParameter != null && directTypeParameter in trackedParameters) {
            occurrences.getOrPut(directTypeParameter, ::mutableListOf)
                .add(CommonTypeVariableOccurrence(specificType, positionVariance))
            return
        }

        when (generalType) {
            is ConeFunctionType -> {
                val specificFunctionType = specificType as? ConeFunctionType ?: return
                if (generalType.parameterTypes.size != specificFunctionType.parameterTypes.size) return
                for (index in generalType.parameterTypes.indices) {
                    collectCommonTypeVariableOccurrences(
                        specificType = specificFunctionType.parameterTypes[index],
                        generalType = generalType.parameterTypes[index],
                        positionVariance = positionVariance.flip(),
                        trackedParameters = trackedParameters,
                        occurrences = occurrences,
                    )
                }
                collectCommonTypeVariableOccurrences(
                    specificType = specificFunctionType.returnType,
                    generalType = generalType.returnType,
                    positionVariance = positionVariance,
                    trackedParameters = trackedParameters,
                    occurrences = occurrences,
                )
            }

            is ConeTupleType -> {
                val specificTupleType = specificType as? ConeTupleType ?: return
                if (generalType.elementTypes.size != specificTupleType.elementTypes.size) return
                for (index in generalType.elementTypes.indices) {
                    collectCommonTypeVariableOccurrences(
                        specificType = specificTupleType.elementTypes[index],
                        generalType = generalType.elementTypes[index],
                        positionVariance = positionVariance,
                        trackedParameters = trackedParameters,
                        occurrences = occurrences,
                    )
                }
            }

            is ConeVArrayType -> {
                val specificVArrayType = specificType as? ConeVArrayType ?: return
                collectCommonTypeVariableOccurrences(
                    specificType = specificVArrayType.elementType,
                    generalType = generalType.elementType,
                    positionVariance = positionVariance.invariant(),
                    trackedParameters = trackedParameters,
                    occurrences = occurrences,
                )
            }

            else -> collectInvariantTypeArgumentOccurrences(
                specificType = specificType,
                generalType = generalType,
                trackedParameters = trackedParameters,
                occurrences = occurrences,
            )
        }
    }

    /**
     * 按不变位置递归收集普通类型实参中的公共类型变元出现。
     */
    private fun collectInvariantTypeArgumentOccurrences(
        specificType: ConeCangJieType,
        generalType: ConeCangJieType,
        trackedParameters: Set<ConeTypeParameterLookupTag>,
        occurrences: MutableMap<ConeTypeParameterLookupTag, MutableList<CommonTypeVariableOccurrence>>,
    ) {
        val specificArguments = specificType.typeArguments
        val generalArguments = generalType.typeArguments
        if (specificArguments.size != generalArguments.size) return

        for (index in generalArguments.indices) {
            collectCommonTypeVariableOccurrences(
                specificType = specificArguments[index].type,
                generalType = generalArguments[index].type,
                positionVariance = TypePositionVariance.INVARIANT,
                trackedParameters = trackedParameters,
                occurrences = occurrences,
            )
        }
    }

    /**
     * 从 Cone 类型中提取直接表示类型参数的 lookup tag。
     */
    private fun ConeCangJieType.typeParameterLookupTagOrNull(): ConeTypeParameterLookupTag? {
        return when (this) {
            is ConeTypeParameterType -> lookupTag
            is ConeTypeVariableType -> typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            else -> null
        }
    }

    /**
     * 对齐官方 `LocalTypeArgumentSynthesis::UnifyContextTyVar`：
     * 当 specific 候选的参数类型本身是声明类型参数时，它是上下文泛型，
     * 不能作为裸类型去匹配 general 候选的非占位参数类型，必须先提升为声明上界。
     */
    private fun satisfiesCangjieContextTypeVariableRule(
        specificType: ConeCangJieType,
        generalType: ConeCangJieType,
        trackedParameters: Set<ConeTypeParameterLookupTag>,
    ): Boolean {
        if (generalType.isDirectTrackedTypeParameter(trackedParameters)) return true

        val promotedSpecificType = (specificType as? ConeTypeParameterType)
            ?.contextTypeVariableUpperBoundForComparison()
            ?: return true

        return createEmptyConstraintSystem().isSignatureEquallyOrMoreSpecific(
            FlatSignature(
                origin = Unit,
                typeParameters = emptyList(),
                valueParameterTypes = listOf(promotedSpecificType),
                hasExtensionReceiver = false,
                contextReceiverCount = 0,
                hasVarargs = false,
                numDefaults = 0,
                isExpect = false,
                isSyntheticMember = false,
            ),
            FlatSignature(
                origin = Unit,
                typeParameters = trackedParameters,
                valueParameterTypes = listOf(generalType),
                hasExtensionReceiver = false,
                contextReceiverCount = 0,
                hasVarargs = false,
                numDefaults = 0,
                isExpect = false,
                isSyntheticMember = false,
            ),
            SpecificityComparisonWithNumerics,
            specificityComparator,
        )
    }

    /**
     * 判断类型是否直接是被跟踪的公共类型参数。
     */
    private fun ConeCangJieType.isDirectTrackedTypeParameter(
        trackedParameters: Set<ConeTypeParameterLookupTag>,
    ): Boolean {
        return typeParameterLookupTagOrNull() in trackedParameters
    }

    /**
     * 将上下文类型变量提升为声明上界，用于官方公共类型变元统一规则。
     */
    private fun ConeTypeParameterType.contextTypeVariableUpperBoundForComparison(): ConeCangJieType {
        lookupTag.typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val bounds = lookupTag.typeParameterSymbol.resolvedBounds
            .map { it.coneType }
            .filterNot { it is ConeErrorType }

        return when (bounds.size) {
            0 -> ConeAnyType
            1 -> bounds.single()
            else -> ConeIntersectionType(bounds)
        }
    }

    /**
     * 判断同一个公共类型变元的多次出现能否选择出一致目标类型。
     */
    private fun hasConsistentCommonTypeVariableTarget(
        occurrences: List<CommonTypeVariableOccurrence>,
    ): Boolean {
        val invariantTypes = occurrences
            .filter { it.variance == TypePositionVariance.INVARIANT }
            .map { it.specificType }
        val covariantTypes = occurrences
            .filter { it.variance == TypePositionVariance.COVARIANT }
            .map { it.specificType }
        val contravariantTypes = occurrences
            .filter { it.variance == TypePositionVariance.CONTRAVARIANT }
            .map { it.specificType }

        val targetTypes = when {
            invariantTypes.isNotEmpty() -> invariantTypes
            else -> covariantTypes + contravariantTypes
        }.distinct()

        return targetTypes.any { targetType ->
            invariantTypes.all { isSameCangjieType(it, targetType) } &&
                covariantTypes.all { isCangjieSubtypeOf(it, targetType) } &&
                contravariantTypes.all { isCangjieSubtypeOf(targetType, it) }
        }
    }

    /**
     * 判断两个仓颉类型是否相同。
     */
    private fun isSameCangjieType(first: ConeCangJieType, second: ConeCangJieType): Boolean {
        return AbstractTypeChecker.equalTypes(inferenceComponents.session.typeContext, first, second)
    }

    /**
     * 判断 `subType` 是否可视为 `superType` 的仓颉子类型，包含数值特异性补充规则。
     */
    private fun isCangjieSubtypeOf(subType: ConeCangJieType, superType: ConeCangJieType): Boolean {
        return AbstractTypeChecker.isSubtypeOf(inferenceComponents.session.typeContext, subType, superType) ||
            SpecificityComparisonWithNumerics.isNonSubtypeEquallyOrMoreSpecific(subType, superType)
    }

    /**
     * 带仓颉数值特异性补充规则的签名比较回调。
     */
    @Suppress("PrivatePropertyName")
    private val SpecificityComparisonWithNumerics = object : SpecificityComparisonCallbacks {
        /**
         * 判断非子类型场景下的数值类型是否仍可视为 equally-or-more-specific。
         */
        override fun isNonSubtypeEquallyOrMoreSpecific(
            specific: CangJieTypeMarker,
            general: CangJieTypeMarker,
        ): Boolean {
            val specificType = specific as? ConeCangJieType ?: return false
            val generalType = general as? ConeCangJieType ?: return false
            val specificPrimitive = specificType.fullyExpandedType() as? ConePrimitiveType ?: return false
            val generalPrimitive = generalType.fullyExpandedType() as? ConePrimitiveType ?: return false

            return isPrimitiveEquallyOrMoreSpecific(specificPrimitive.kind, generalPrimitive.kind)
        }
    }

    /**
     * 为候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate): FlatSignature<Candidate> {
        if (!call.symbol.isBound) {
            return FlatSignature(
                origin = call,
                typeParameters = emptyList(),
                valueParameterTypes = emptyList<TypeWithConversion>(),
                hasExtensionReceiver = false,
                contextReceiverCount = 0,
                hasVarargs = false,
                numDefaults = call.numDefaults,
                isExpect = false,
                isSyntheticMember = false,
            )
        }

        return when (val declaration = call.symbol.cfir) {
            is CfirConstructor -> createFlatSignature(call, declaration)
            is CfirEnumConstructor -> createFlatSignature(call, declaration)
            is CfirProperty -> createFlatSignature(call, declaration)
            is CfirFunction -> createFlatSignature(call, declaration)

            is CfirVariable -> createFlatSignature(call, declaration)
            is CfirClassLikeDeclaration -> createFlatSignature(call, declaration)
            else -> error("Unsupported declaration for overload conflict resolution: ${declaration::class.java.name}")
        }
    }

    /**
     * 为函数声明候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate, declaration: CfirFunction): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = call.typeParametersForSignature(declaration),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = call.usesVariadicCall || call.usesCangjieVariadicCall,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    /**
     * 为普通构造器候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate, declaration: CfirConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = call.typeParametersForSignature(declaration),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = call.usesVariadicCall || call.usesCangjieVariadicCall,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    /**
     * 为 enum constructor 候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate, declaration: CfirEnumConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = call.typeParametersForSignature(declaration),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = call.usesVariadicCall,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    /**
     * 为属性候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate, declaration: CfirProperty): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = call.typeParametersForSignature(declaration),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = call.usesVariadicCall || call.usesCangjieVariadicCall,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    /**
     * 为变量候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate, declaration: CfirVariable): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = call.typeParametersForSignature(declaration),
            valueParameterTypes = computeSignatureTypes(call, declaration),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = call.numDefaults,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    /**
     * 为 class-like 候选创建平铺签名。
     */
    private fun createFlatSignature(call: Candidate, declaration: CfirClassLikeDeclaration): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = declaration.typeParameters().toTypeParameterMarkers(),
            valueParameterTypes = emptyList<TypeWithConversion>(),
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = false,
            numDefaults = 0,
            isExpect = false,
            isSyntheticMember = declaration.origin is CfirDeclarationOrigin.Synthetic,
        )
    }

    /**
     * 根据候选实际实参映射或声明参数计算签名参数类型列表。
     */
    private fun computeSignatureTypes(
        call: Candidate,
        called: CfirCallableDeclaration,
    ): List<TypeWithConversion> {
        return buildList {
            val session = inferenceComponents.session
            val typeForCallableReference = call.resultingTypeForCallableReference
            if (typeForCallableReference != null) {
                val functionType = typeForCallableReference.fullyExpandedType()
                if (functionType is ConeFunctionType) {
                    functionType.parameterTypes.mapTo(this) { parameterType ->
                        TypeWithConversion(parameterType.prepareCallableReferenceSignatureType(session, call))
                    }
                }
            } else if (call.argumentMappingInitialized) {
                val variadicParameter = call.cangjieVariadicParameterForCall
                var variadicParameterAdded = false
                for ((argument, parameter) in call.argumentMapping) {
                    if (parameter == variadicParameter) {
                        if (variadicParameterAdded) continue
                        variadicParameterAdded = true
                    }
                    val signatureType = call.variadicExpectedTypeForArgument(argument)?.let { variadicExpectedType ->
                        TypeWithConversion(variadicExpectedType.prepareType(session, call))
                    } ?: parameter.toTypeWithConversion(argument, session, call)
                    add(signatureType)
                }
            } else {
                declaredParametersFor(called).mapTo(this) { parameter ->
                    val parameterType = parameter.returnTypeRef.coneTypeOrNull()?.prepareType(session, call)
                    TypeWithConversion(parameterType)
                }
            }
        }
    }

    /**
     * 返回 callable 声明中的值参数列表。
     */
    private fun declaredParametersFor(called: CfirCallableDeclaration): List<CfirValueParameter> {
        return when (called) {
            is CfirConstructor -> called.valueParameters
            is CfirEnumConstructor -> called.valueParameters
            is CfirFunction -> called.valueParameters

            else -> emptyList()
        }
    }

    /**
     * 把值参数转换为签名比较使用的类型及转换信息。
     */
    private fun CfirValueParameter.toTypeWithConversion(
        argument: org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom,
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        call: Candidate,
    ): TypeWithConversion {
        val argumentType = returnTypeRef.coneTypeOrNull()?.prepareType(session, call)
        val functionTypeForSam = toFunctionTypeForSamOrNull(argument, call)?.prepareType(session, call)
        return if (functionTypeForSam == null) {
            TypeWithConversion(argumentType)
        } else {
            TypeWithConversion(functionTypeForSam, argumentType)
        }
    }

    /**
     * 查询 SAM 转换实参对应的函数类型。
     */
    private fun CfirValueParameter.toFunctionTypeForSamOrNull(
        argument: org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom,
        call: Candidate,
    ): ConeCangJieType? {
        val functionTypesOfSamConversions = call.samConversionInfosOfArguments ?: return null
        return functionTypesOfSamConversions[argument.expression]?.functionalType
    }

    /**
     * 准备签名比较中的参数类型。
     */
    private fun ConeCangJieType.prepareType(
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        candidate: Candidate,
    ): ConeCangJieType {
        val expanded = fullyExpandedType()
        if (!candidate.system.usesOuterCs) return expanded

        val substitutor = candidate.system.buildNotFixedVariablesToStubTypesSubstitutor()
        return with(session.typeContext) {
            substitutor.safeSubstitute(expanded) as ConeCangJieType
        }
    }

    /**
     * callable reference 候选签名比较需要看到候选当前约束系统已经求出的类型。
     *
     * 泛型函数引用没有源码类型实参位置；若直接把未固定 fresh variable 替换为 stub，
     * `println<T>` 这类泛型候选会和 `println(Int64)` 这类专门候选在特异性比较中保持并列。
     * 这里先把对应声明类型参数的 fresh variable 规整回源码类型参数，避免它们被
     * callable reference 的 expected type 约束提前解成与专门重载相同的参数类型；
     * 随后再应用当前替换，让外层非声明推断变量继续参与精确比较，最后才用 stub
     * 表示真正无法参与精确比较的推断变量。
     */
    private fun ConeCangJieType.prepareCallableReferenceSignatureType(
        session: org.cangnova.cangjie.cfir.session.CfirSession,
        candidate: Candidate,
    ): ConeCangJieType {
        val declarationTypeParameterBindings = candidate.declarationTypeParameterBindingsForCallableReference()
        val expanded = fullyExpandedType().restoreDeclarationTypeParameters()
        val currentSubstituted = with(session.typeContext) {
            candidate.system
                .buildCurrentSubstitutor(declarationTypeParameterBindings)
                .safeSubstitute(expanded) as ConeCangJieType
        }
        if (!candidate.system.usesOuterCs) return currentSubstituted

        val notFixedToStub = candidate.system.buildNotFixedVariablesToStubTypesSubstitutor()
        return with(session.typeContext) {
            notFixedToStub.safeSubstitute(currentSubstituted) as ConeCangJieType
        }
    }

    /**
     * callable reference most-specific 比较中，候选自身的声明类型参数必须以声明类型参数身份参与比较。
     *
     * 候选可用 expected function type 证明自身适用，但不能因此把 `println<T>(T)` 的 `T`
     * 固定成 `Int64` 后与 `println(Int64)` 变成同一个签名；否则专门重载会被误判为歧义。
     * 这里同时覆盖 fresh constructor 和源码 lookup tag 两种 substitutor 键，保证当前替换只作用于外层推断变量。
     */
    private fun Candidate.declarationTypeParameterBindingsForCallableReference(): Map<TypeConstructorMarker, ConeCangJieType> {
        val bindings = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()
        for (freshVariable in freshVariables) {
            val originalTypeParameter = freshVariable.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                ?: continue
            val declarationType = ConeTypeParameterTypeImpl(originalTypeParameter, freshVariable.defaultType.attributes)
            bindings[freshVariable.typeConstructor] = declarationType
            bindings[originalTypeParameter] = declarationType
        }
        return bindings
    }

    /** 递归把候选 fresh type variable 还原为其声明侧类型参数。 */
    private fun ConeCangJieType.restoreDeclarationTypeParameters(): ConeCangJieType {
        fun List<ConeTypeProjection>.restoreArguments(): List<ConeTypeProjection> =
            map { projection -> projection.type.restoreDeclarationTypeParameters() }

        return when (this) {
            is ConeTypeVariableType -> {
                val originalTypeParameter = typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                if (originalTypeParameter != null) {
                    ConeTypeParameterTypeImpl(originalTypeParameter, attributes)
                } else {
                    this
                }
            }
            is ConeClassLikeType -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments.restoreArguments(),
                attributes = attributes,
                isInterface = isInterface,
                isThisType = isThisType,
            )
            is ConeStructType -> ConeStructType(
                lookupTag = lookupTag,
                typeArguments = typeArguments.restoreArguments(),
                attributes = attributes,
            )
            is ConeEnumType -> ConeEnumType(
                lookupTag = lookupTag,
                typeArguments = typeArguments.restoreArguments(),
                attributes = attributes,
                isRefEnum = isRefEnum,
            )
            is ConeTypeAliasType -> ConeTypeAliasType(
                classId = classId,
                expandedType = expandedType?.restoreDeclarationTypeParameters(),
                typeArguments = typeArguments.restoreArguments(),
                attributes = attributes,
            )
            is ConeFunctionType -> ConeFunctionType(
                parameterTypes = parameterTypes.map { it.restoreDeclarationTypeParameters() },
                returnType = returnType.restoreDeclarationTypeParameters(),
                isCFunc = isCFunc,
                isClosureType = isClosureType,
                hasVariableLenArg = hasVariableLenArg,
                attributes = attributes,
            )
            is ConeTupleType -> ConeTupleType(
                elementTypes = elementTypes.map { it.restoreDeclarationTypeParameters() },
                attributes = attributes,
            )
            is ConeVArrayType -> ConeVArrayType(
                elementType = elementType.restoreDeclarationTypeParameters(),
                size = size,
                attributes = attributes,
            )
            is ConePointerType -> ConePointerType(
                pointeeType = pointeeType.restoreDeclarationTypeParameters(),
                attributes = attributes,
            )
            else -> this
        }
    }

    /**
     * 递归展开 typealias 类型。
     */
    private fun ConeCangJieType.fullyExpandedType(): ConeCangJieType {
        return when (this) {
            is ConeTypeAliasType -> expandedType?.fullyExpandedType() ?: this
            else -> this
        }
    }

    /**
     * 收集候选签名中用于 fresh variable 的类型参数。
     */
    private fun Candidate.typeParametersForSignature(declaration: Any?): List<TypeParameterMarker> {
        return CfirCreateFreshTypeVariableSubstitutorStage
            .collectCandidateTypeParametersForFreshVariables(inferenceComponents.session, this, declaration)
            .toTypeParameterMarkers()
    }

    /**
     * 将 CFIR 类型参数引用转换为通用类型系统 marker。
     */
    private fun List<CfirTypeParameterRef>.toTypeParameterMarkers(): List<TypeParameterMarker> {
        return mapNotNull { it.symbol.toLookupTag() as? TypeParameterMarker }
    }

    /**
     * 返回 class-like 声明自身的类型参数列表。
     */
    private fun CfirClassLikeDeclaration.typeParameters(): List<CfirTypeParameter> {
        return when (this) {
            is CfirClass -> typeParameters
            is CfirPrimitiveTypeDeclaration -> emptyList()
            is CfirInterface -> typeParameters
            is CfirStruct -> typeParameters
            is CfirEnum -> typeParameters
            is CfirTypeAlias -> typeParameters
        }
    }

    /**
     * 创建用于签名比较的空约束系统。
     */
    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(inferenceComponents.createConstraintSystem(), inferenceComponents)
    }

    /**
     * 判断 primitive 数值类型在仓颉重载特异性规则下是否不弱于目标类型。
     */
    private fun isPrimitiveEquallyOrMoreSpecific(
        specific: PrimitiveTypeKind,
        general: PrimitiveTypeKind,
    ): Boolean {
        if (!specific.isNumeric || !general.isNumeric) return false
        if (specific == general) return true

        // 对齐官方仓颉 TypeCheckUtil::CompareIntAndFloat：
        // Int64 优先于其他整数，整数优先于浮点，Float64 优先于其他浮点；
        // 除这些规则外，同族数值类型在重载消歧中互为等价。
        return when {
            specific.isInteger -> {
                if (general.isInteger) {
                    specific == PrimitiveTypeKind.INT64 || general != PrimitiveTypeKind.INT64
                } else {
                    true
                }
            }

            general.isInteger -> false

            specific == PrimitiveTypeKind.FLOAT64 -> true
            general == PrimitiveTypeKind.FLOAT64 -> false
            else -> true
        }
    }

    /**
     * 带原始 base scope 的成员符号。
     */
    private data class MemberWithBaseScope<S : CfirCallableSymbol<*>>(
        /** 当前成员符号。 */
        val symbol: S,
        /** 产生当前成员的类型 scope。 */
        val scope: CfirTypeScope,
    )

    /**
     * 遍历直接 overridden 成员的函数式接口。
     */
    private fun interface ProcessAllOverridden<S : CfirCallableSymbol<*>> {
        /**
         * 处理指定成员的直接 overridden 符号。
         */
        fun process(
            scope: CfirTypeScope,
            symbol: S,
            processor: (S, CfirTypeScope) -> ProcessorAction,
        ): ProcessorAction
    }

    /**
     * 递归判断成员是否 override 目标符号。
     */
    private fun <S : CfirCallableSymbol<*>> overrides(
        member: MemberWithBaseScope<S>,
        target: CfirCallableSymbol<*>,
        overriddenProducer: ProcessAllOverridden<S>,
    ): Boolean {
        val visited = linkedSetOf<S>()

        /**
         * 深度遍历 overridden 链。
         */
        fun visit(current: MemberWithBaseScope<S>): Boolean {
            if (!visited.add(current.symbol)) return false

            var found = false
            overriddenProducer.process(current.scope, current.symbol) { overridden, baseScope ->
                when {
                    overridden == target -> {
                        found = true
                        ProcessorAction.STOP
                    }

                    visit(MemberWithBaseScope(overridden, baseScope)) -> {
                        found = true
                        ProcessorAction.STOP
                    }

                    else -> ProcessorAction.NEXT
                }
            }
            return found
        }

        return visit(member)
    }

}

/**
 * 从 resolved type ref 中读取 Cone 类型。
 */
private fun CfirTypeRef.coneTypeOrNull(): ConeCangJieType? {
    return (this as? CfirResolvedTypeRef)?.coneType
}

/**
 * 签名比较使用的简化约束系统实现。
 */
private class ConeSimpleConstraintSystemImpl(
    /** 底层约束系统实现。 */
    private val system: ConstraintSystemImpl,
    /** 推断组件集合。 */
    private val inferenceComponents: InferenceComponents,
) : SimpleConstraintSystem {
    /**
     * 为签名比较注册类型变量，并返回类型参数到 fresh 变量默认类型的 substitutor。
     */
    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker {
        val builder = system.getBuilder()
        val substitutionMap = linkedMapOf<org.cangnova.cangjie.type.model.TypeConstructorMarker, ConeCangJieType>()

        for (typeParameter in typeParameters) {
            require(typeParameter is ConeTypeParameterLookupTag)
            val variable = ConeTypeParameterBasedTypeVariable(typeParameter.typeParameterSymbol)
            builder.registerVariable(variable)
            substitutionMap[typeParameter] = variable.defaultType
        }

        val substitutor = createTypeSubstitutorByTypeConstructor(
            map = substitutionMap,
            context = inferenceComponents.session.typeContext,
            approximateIntegerLiterals = false,
        )

        for (typeParameter in typeParameters) {
            require(typeParameter is ConeTypeParameterLookupTag)
            val variableType = substitutionMap[typeParameter]
                ?: error("Missing substituted variable for $typeParameter")
            for (upperBound in typeParameter.typeParameterSymbol.resolvedBounds) {
                addSubtypeConstraint(
                    variableType,
                    with(inferenceComponents.session.typeContext) {
                        substitutor.safeSubstitute(upperBound.coneType) as ConeCangJieType
                    },
                )
            }
        }

        return substitutor
    }

    /**
     * 添加 subtype 约束。
     */
    override fun addSubtypeConstraint(subType: CangJieTypeMarker, superType: CangJieTypeMarker) {
        system.addSubtypeConstraint(subType, superType, SimpleConstraintSystemConstraintPosition)
    }

    /**
     * 当前约束系统是否存在矛盾。
     */
    override fun hasContradiction(): Boolean = system.hasContradiction


    /** 类型系统推断扩展上下文。 */
    override val context: TypeSystemInferenceExtensionContext
        get() = system

    /** 通用约束系统 marker 视图。 */
    override val constraintSystemMarker: org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
        get() = system
}
