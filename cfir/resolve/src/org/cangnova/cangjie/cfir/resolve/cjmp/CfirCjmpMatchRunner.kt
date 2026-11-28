package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.resolve.calls.mpp.*

/**
 * 声明级匹配输入准备，CLI 与 LL 共用。只读取当前 root、原 common 候选与真实外层事实；
 * 不枚举或发布同名 specific 兄弟，不求值组占用，也不写配对表。
 */
object CfirCjmpMatchRunner {
    fun prepareDeclaration(
        declaration: CfirMemberDeclaration,
        session: CfirSession,
        preparedOwnerFacts: CfirCjmpDeclarationMatchFacts? = null,
        preparedCommonCandidates: List<CfirDeclaration>? = null,
        matchingMode: CfirCjmpMatchingMode = CfirCjmpMatchingMode.FINAL,
    ): CfirCjmpDeclarationMatchFacts {
        declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val owned = buildList<CfirBasedSymbol<*>> {
            add(declaration.symbol)
            when (declaration) {
                is CfirProperty -> {
                    declaration.getter?.let { add(it.symbol) }
                    declaration.setter?.let { add(it.symbol) }
                }
                is CfirPatternVariable -> addAll(declaration.pattern.bindingVariables().map { it.symbol })
                else -> Unit
            }
        }
        if (!declaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) || !canHaveCommonCounterpart(declaration)) {
            return CfirCjmpDeclarationMatchFacts(declaration.symbol, owned, emptyList(), declaration.cjmpOriginalContainerInput(), emptyList())
        }
        val ownContainer = (declaration as? CfirCallableDeclaration)?.getContainingCjmpDeclaration()
        val ownContainerFacts = ownContainer?.let { owner ->
            preparedOwnerFacts?.takeIf { it.rootSymbol === owner.symbol }
                ?: checkNotNull(owner.cjmpMatchFacts) { "A member requires its original outer CJMP facts" }
        }
        val candidates = preparedCommonCandidates
            ?: CfirCjmpResolver.findCommonCandidatesForDeclaration(declaration, session, ownContainerFacts)
        candidates.filterIsInstance<CfirMemberDeclaration>().forEach { it.lazyResolveToPhase(CfirResolvePhase.TYPES) }
        val traversal = if (session.hasMultipleCjmpCommonParents()) {
            CjmpCandidateTraversal.ALL_CANDIDATES
        } else {
            CjmpCandidateTraversal.FIRST_SUCCESS
        }
        val container = declaration.cjmpOriginalContainerInput()
        val commonContainers = if (container != null) candidates.mapNotNull { it.cjmpOriginalContainerInput() } else emptyList()
        fun parentMapping(common: CfirDeclaration): Map<CfirTypeParameter, CfirTypeParameter> {
            if (ownContainerFacts == null) return emptyMap()
            // 同包依赖可含多个同名 nominal；按冻结的物理成员关系定位 owner，避免 ClassId 查询落到另一个父项。
            val originalCommon = ownContainerFacts.commonContainers.single { commonContainer ->
                commonContainer.members.any { it.symbol === common.symbol }
            }
            val originalSpecific = checkNotNull(ownContainerFacts.container)
            return originalCommon.typeParameters.zip(originalSpecific.typeParameters)
                .associate { (from, to) -> from.cfir to to.cfir }
        }
        val plan: CjmpMatchPlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing> = when (declaration) {
            is CfirPatternVariable -> CfirCjmpResolver.buildPatternMatchPlan(
                declaration, candidates.filterIsInstance<CfirVariable>(), session, emptyMap(), traversal,
            )
            is CfirProperty -> {
                val plans = candidates.filterIsInstance<CfirProperty>().map { common ->
                    CfirCjmpResolver.buildPropertyMatchPlan<Nothing>(
                        declaration, listOf(CfirCjmpPropertyCandidate(common)), session, traversal, parentMapping(common),
                    )
                }
                CjmpMatchPlan.Declaration(
                    declaration.symbol,
                    emptyList(),
                    plans.flatMap { it.candidates },
                    traversal,
                    checkParameterDefaultValueConflict = session.languageVersionSettings
                        .supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules),
                )
            }
            is CfirCallableDeclaration -> {
                val specificCallableKind = CfirCjmpMatcher.callableKind(declaration)
                val callableCandidates = if (specificCallableKind == null) {
                    emptyList()
                } else {
                    candidates.filterIsInstance<CfirCallableDeclaration>().filter { common ->
                        CfirCjmpMatcher.callableKind(common) == specificCallableKind
                    }
                }
                val plans = callableCandidates.map { common ->
                    CfirCjmpResolver.buildCallableMatchPlan(
                        declaration, cfirCjmpDefaultStates(declaration), listOf(callableCandidate(common)), session,
                        traversal, parentMapping(common), matchingMode,
                    )
                }
                CjmpMatchPlan.Declaration(
                    declaration.symbol,
                    cfirCjmpDefaultStates(declaration),
                    plans.flatMap { it.candidates },
                    traversal,
                    checkParameterDefaultValueConflict = session.languageVersionSettings
                        .supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules),
                )
            }
            else -> {
                val context = CfirCjmpMatchingContext(session)
                val plans = candidates.map { common ->
                    val facts = AbstractCjmpMatcher.prepareCandidate(declaration.symbol, common.symbol, context)
                    CjmpCandidatePlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Nothing>(
                        common.symbol, facts.eligibility, emptyList(), facts.signatureCompatibility,
                        emptyList(), emptyList(), CjmpMatchResult.Matched, emptyList(),
                        CjmpBindingRule.MergedContainer(CjmpBindingRequest(declaration.symbol, common.symbol, facts.typeParameterMapping, false)),
                    )
                }
                CjmpMatchPlan.Declaration(
                    specific = declaration.symbol,
                    initialDefaults = emptyList(),
                    candidates = plans,
                    traversal = CjmpCandidateTraversal.ALL_CANDIDATES,
                    checkParameterDefaultValueConflict = session.languageVersionSettings
                        .supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules),
                )
            }
        }
        return CfirCjmpDeclarationMatchFacts(declaration.symbol, owned, listOf(plan), container, commonContainers)
    }

    /** 对位 Kotlin canHaveExpectCounterPart；仓颉额外的 extend、pattern 仍保留各自真实阶段根。 */
    fun canHaveCommonCounterpart(declaration: CfirDeclaration): Boolean = when (declaration) {
        is CfirTypeAlias -> false
        is CfirClassLikeDeclaration, is CfirExtend, is CfirPatternVariable -> true
        is CfirConstructor -> !declaration.status.isStatic
        is CfirCallableDeclaration -> declaration !is CfirPropertyAccessor
        else -> false
    }
}

/** 默认值状态保存真实 helper 符号，不能把参数或占位表达式冒充 desugar 函数。 */
internal fun cfirCjmpDefaultStates(function: CfirCallableDeclaration): List<CjmpParameterDefaultState<CfirBasedSymbol<*>>> =
    function.cjmpParametersForMatching().map(CfirValueParameter::declaredCjmpDefaultParameterState)

/** 字段在 callable 层级参与成员匹配，但没有函数形参或默认值角色事实。 */
internal fun CfirCallableDeclaration.cjmpParametersForMatching(): List<CfirValueParameter> =
    if (this is CfirVariable) emptyList() else cjmpValueParameters()

private fun callableCandidate(common: CfirCallableDeclaration): CfirCjmpCallableCandidate<Nothing> = CfirCjmpCallableCandidate(
    common, cfirCjmpDefaultStates(common), emptyList(), emptyList(), CjmpMatchResult.Matched, emptyList(),
)
