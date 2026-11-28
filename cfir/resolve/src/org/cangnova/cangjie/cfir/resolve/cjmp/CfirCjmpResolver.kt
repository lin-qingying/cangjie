/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCjmpDeclarationMatchFacts
import org.cangnova.cangjie.cfir.declarations.cjmpValueParameters
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirCjmpMatchingMode
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.session.getContainingCjmpDeclaration
import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.expandedExtendTargetKey
import org.cangnova.cangjie.resolve.calls.mpp.AbstractCjmpMatcher
import org.cangnova.cangjie.resolve.calls.mpp.CjmpBindingRequest
import org.cangnova.cangjie.resolve.calls.mpp.CjmpBindingRule
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCandidatePlan
import org.cangnova.cangjie.resolve.calls.mpp.CjmpCandidateTraversal
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchPlan
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchResult
import org.cangnova.cangjie.resolve.calls.mpp.CjmpParameterDefaultState
import org.cangnova.cangjie.resolve.calls.mpp.CjmpParameterMatch
import org.cangnova.cangjie.resolve.calls.mpp.CjmpVariableAssignmentKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpPatternMatchElement
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile

/**
 * CJMP common 候选查找（对齐 Kotlin `FirExpectActualResolver.findExpectForActual`）。
 *
 * 架构要点（G3 修复）：
 * - 一律经 **依赖侧** 符号提供器（[CfirSession.dependenciesSymbolProvider]）查找；
 *   绝不会命中 specific 模块自身的同名声明（普通查询"本模块优先"的顺序在此不适用）；
 * - 候选过滤：`status.isCommon && moduleData ∈ 传递 dependsOn 依赖`（DAG 语义，D3：官方多 parent 链）；
 * - 模块同一性：使用 CfirModuleData 的身份/等值契约；LL 包装由底层 caModule 提供稳定身份。
 *   名称只用于呈现，不能把同名模块混入 refinement 依赖或本模块声明组。
 *
 * 成员查找对位 Kotlin `expectContainingClass.getCallablesForExpectClass(name)`：
 * 先从 specific 声明的真实 owner 取得已配对 common 容器，再按名筛选其成员。
 */
object CfirCjmpResolver {
    private fun CfirSession.checkParameterDefaultValueConflict(): Boolean =
        languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules)

    /**
     * 保留官方 VAR / VarWithPattern 的身份与直接元素边界。
     * 普通顶层变量的外壳不是 native VAR，配对键必须是其中原有的 binding 声明；
     * tuple 不展开嵌套模式，停止点后的声明不会被求值器访问。
     */
    fun <Diagnostic> buildPatternMatchPlan(
        specific: CfirPatternVariable,
        candidates: List<CfirVariable>,
        session: CfirSession,
        parentTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
        traversal: CjmpCandidateTraversal,
    ): CjmpMatchPlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic> {
        val commonVariables = candidates.flatMap { candidate ->
            if (candidate is CfirPatternVariable) {
                candidate.pattern.cjmpBindingVariables()
            } else {
                listOf(candidate)
            }
        }
        fun variablePlan(binding: CfirPatternBindingVariable) = buildVariableMatchPlan<Diagnostic>(
            specific = binding,
            candidates = commonVariables.filter { it.symbol.name == binding.name }.map(::CfirCjmpVariableCandidate),
            session = session,
            traversal = traversal,
            parentTypeParameterMapping = parentTypeParameterMapping,
        )

        return when (val pattern = specific.pattern) {
            is CfirBindingPattern if pattern.nestedPattern == null ->
                variablePlan(requireNotNull(pattern.bindingVariable) { "A declaration binding must own its variable" })
            is CfirTuplePattern -> CjmpMatchPlan.TuplePattern(
                specific.symbol,
                pattern.elements.map { element ->
                    if (element is CfirBindingPattern && element.nestedPattern == null) {
                        CjmpPatternMatchElement.Variable(
                            variablePlan(requireNotNull(element.bindingVariable) { "A declaration binding must own its variable" }),
                        )
                    } else {
                        CjmpPatternMatchElement.NonVariable()
                    }
                },
            )
            else -> CjmpMatchPlan.NonTuplePattern(specific.symbol)
        }
    }

    /**
     * 把 static/global 变量的候选冻结为有序程序。
     *
     * 官方把变量分两条路（`CheckCJMP.cpp:586-591` 的注释）：**实例成员变量在容器合并时配对**，
     * **global 与 static 在 Match 阶段配对**。本工厂只覆盖后者，绑定种类为
     * `VariableAssignment(STATIC_OR_GLOBAL)`——官方在该分支写 `specificImplementation` 并置
     * `doNotExport = true`。
     *
     * 实例成员变量（`MergedMemberVariable`）要求其绑定由容器合并的 seed 预先建立，
     * 那需要先接入 `CjmpContainerMergeEvaluator` 的 `initialBindings`；在补齐之前仍走既有直连路径。
     */
    fun <Diagnostic> buildVariableMatchPlan(
        specific: CfirVariable,
        candidates: List<CfirCjmpVariableCandidate>,
        session: CfirSession,
        traversal: CjmpCandidateTraversal,
        parentTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic> {
        val context = CfirCjmpMatchingContext(session)
        val parentMapping = context.parentTypeParameterMapping(parentTypeParameterMapping)
        val plans = candidates.map { candidate ->
            val common = candidate.common
            val facts = AbstractCjmpMatcher.prepareCandidate<CfirBasedSymbol<*>, CfirTypeParameterSymbol, org.cangnova.cangjie.cfir.types.ConeCangJieType>(
                specific.symbol, common.symbol, context, parentMapping,
            )
            val binding = bindingRule(
                specific, common,
                request = CjmpBindingRequest<CfirBasedSymbol<*>, CfirTypeParameterSymbol>(
                    specific = specific.symbol,
                    common = common.symbol,
                    typeParameterMapping = facts.typeParameterMapping,
                    requiresImplementation = false,
                ),
            )
            val signatureCompatibility = if (
                facts.eligibility == CjmpMatchResult.Matched && binding is CjmpBindingRule.VariableAssignment
            ) {
                // 官方 MatchCJMPVar 先保留 static/global implementation；PostTypeCheck 的类型诊断不撤销该关系。
                CjmpMatchResult.Matched
            } else {
                facts.signatureCompatibility
            }
            CjmpCandidatePlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic>(
                common = common.symbol,
                eligibility = facts.eligibility,
                beforeSignatureDiagnostics = emptyList(),
                signatureCompatibility = signatureCompatibility,
                parameters = emptyList(),
                afterParameterDiagnostics = emptyList(),
                afterParameters = CjmpMatchResult.Matched,
                beforeBindingDiagnostics = emptyList(),
                binding = binding,
            )
        }
        return CjmpMatchPlan.Declaration(
            specific = specific.symbol,
            initialDefaults = emptyList(),
            candidates = plans,
            traversal = traversal,
            checkParameterDefaultValueConflict = session.checkParameterDefaultValueConflict(),
        )
    }

    /**
     * 把 specific extend 的有序 common owner 候选冻结为一次完整匹配输入。
     *
     * MergeCJMPExtensions 会把所有同 key common extend 合并进首个 specific extend；
     * 因而这里必须遍历全部有效候选，并将每个 owner 的类型参数映射随绑定一并保留。
     * 类型未解析的 extend 不参与 key 分组，等同于官方在 merge 前跳过该候选。
     */
    fun <Diagnostic> buildExtendMatchPlan(
        specific: CfirExtend,
        candidates: List<CfirExtend>,
        session: CfirSession,
    ): CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic> {
        val context = CfirCjmpMatchingContext(session)
        val candidateMatches = AbstractCjmpMatcher.matchSpecificAgainstPotentialCommon(
            specific = specific.symbol,
            commonCandidates = candidates.map { it.symbol },
            context = context,
        ).filter { it.compatibility != CjmpMatchResult.TypeNotResolved }

        val plans = candidateMatches.map { candidate ->
            CjmpCandidatePlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic>(
                common = candidate.common,
                eligibility = candidate.compatibility,
                beforeSignatureDiagnostics = emptyList(),
                signatureCompatibility = if (candidate.compatibility == CjmpMatchResult.Matched) {
                    CjmpMatchResult.Matched
                } else {
                    null
                },
                parameters = emptyList(),
                afterParameterDiagnostics = emptyList(),
                afterParameters = CjmpMatchResult.Matched,
                beforeBindingDiagnostics = emptyList(),
                binding = CjmpBindingRule.MergedContainer(
                    CjmpBindingRequest(
                        specific = specific.symbol,
                        common = candidate.common,
                        typeParameterMapping = candidate.typeParameterMapping,
                        requiresImplementation = false,
                    ),
                ),
            )
        }
        return CjmpMatchPlan.Declaration(
            specific = specific.symbol,
            initialDefaults = emptyList(),
            candidates = plans,
            traversal = CjmpCandidateTraversal.ALL_CANDIDATES,
            checkParameterDefaultValueConflict = session.checkParameterDefaultValueConflict(),
        )
    }

    /**
     * 把已准备的 property 头与其 getter/setter 冻结为有序程序。
     *
     * 官方 `MatchCJMPProp`（v1.1.3 `CheckCJMP.cpp:1001-1010`）对属性本体与两侧访问器**分别**
     * 调用 `TrySetSpecificImpl`，三步共用同一 `ret`（任一步失败即整体不匹配，但已接受的前缀不回滚）。
     * 本工厂据此为访问器各自建立独立的 `CjmpBindingRequest`：它们有各自的 specific/common 符号，
     * 因此在求值器的占用表里是三个不同的键，不会互相判为第二绑定。
     *
     * 结构资格和类型签名由共享 matcher 一次性准备，不提前执行参数程序。
     */
    fun <Diagnostic> buildPropertyMatchPlan(
        specific: CfirProperty,
        candidates: List<CfirCjmpPropertyCandidate>,
        session: CfirSession,
        traversal: CjmpCandidateTraversal,
        parentTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic> {
        val context = CfirCjmpMatchingContext(session)
        val parentMapping = context.parentTypeParameterMapping(parentTypeParameterMapping)
        val plans = candidates.map { candidate ->
            val common = candidate.common
            val facts = AbstractCjmpMatcher.prepareCandidate<CfirBasedSymbol<*>, CfirTypeParameterSymbol, org.cangnova.cangjie.cfir.types.ConeCangJieType>(
                specific.symbol, common.symbol, context, parentMapping,
            )
            CjmpCandidatePlan<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic>(
                common = common.symbol,
                eligibility = facts.eligibility,
                beforeSignatureDiagnostics = emptyList(),
                signatureCompatibility = facts.signatureCompatibility,
                parameters = emptyList(),
                afterParameterDiagnostics = emptyList(),
                afterParameters = CjmpMatchResult.Matched,
                beforeBindingDiagnostics = emptyList(),
                binding = CjmpBindingRule.Property(
                    property = CjmpBindingRequest<CfirBasedSymbol<*>, CfirTypeParameterSymbol>(
                        specific = specific.symbol,
                        common = common.symbol,
                        typeParameterMapping = facts.typeParameterMapping,
                        requiresImplementation = facts.requiresImplementation,
                    ),
                    // 访问器只在两侧同侧存在时参与：官方按 getters[0]/setters[0] 配对。
                    getter = accessorRequest(specific.getter, common.getter, context, parentMapping),
                    setter = accessorRequest(specific.setter, common.setter, context, parentMapping),
                ),
            )
        }
        return CjmpMatchPlan.Declaration(
            specific = specific.symbol,
            initialDefaults = emptyList(),
            candidates = plans,
            traversal = traversal,
            checkParameterDefaultValueConflict = session.checkParameterDefaultValueConflict(),
        )
    }

    /** 访问器配对请求；两侧缺少对应访问器时不参与（官方只配对 `getters[0]`/`setters[0]`）。 */
    private fun accessorRequest(
        specific: CfirPropertyAccessor?,
        common: CfirPropertyAccessor?,
        context: CfirCjmpMatchingContext,
        parentMapping: Map<CfirTypeParameterSymbol, CfirTypeParameterSymbol>,
    ): CjmpBindingRequest<CfirBasedSymbol<*>, CfirTypeParameterSymbol>? {
        if (specific == null || common == null) return null
        return CjmpBindingRequest<CfirBasedSymbol<*>, CfirTypeParameterSymbol>(
            specific = specific.symbol,
            common = common.symbol,
            typeParameterMapping = AbstractCjmpMatcher
                .typeParameterMapping<CfirBasedSymbol<*>, CfirTypeParameterSymbol, org.cangnova.cangjie.cfir.types.ConeCangJieType>(
                    specific.symbol, common.symbol, parentMapping, context,
                )
                .toMap(),
            requiresImplementation = context.needToReportMissingBody(common.symbol, specific.symbol),
        )
    }

    /**
     * 把已准备的函数头与候选检查事实冻结为有序程序，不解析 body、不转移默认表达式、不发布绑定。
     *
     * 候选资格和类型签名由共享 matcher 一次性冻结；名字与默认来源只生成参数事实，
     * 求值器按位置执行它们，保留失败候选在此前参数上已发生的默认来源转移。
     */
    fun <Diagnostic> buildCallableMatchPlan(
        specific: CfirCallableDeclaration,
        specificDefaults: List<CjmpParameterDefaultState<CfirBasedSymbol<*>>>,
        candidates: List<CfirCjmpCallableCandidate<Diagnostic>>,
        session: CfirSession,
        traversal: CjmpCandidateTraversal,
        parentTypeParameterMapping: Map<CfirTypeParameter, CfirTypeParameter>,
        matchingMode: CfirCjmpMatchingMode = CfirCjmpMatchingMode.FINAL,
    ): CjmpMatchPlan.Declaration<CfirBasedSymbol<*>, CfirTypeParameterSymbol, Diagnostic> {
        val context = CfirCjmpMatchingContext(session, matchingMode)
        val parentMapping = context.parentTypeParameterMapping(parentTypeParameterMapping)
        val specificParameters = specific.cjmpParametersForMatching()
        require(specificDefaults.size == specificParameters.size) { "Every specific parameter must have explicit default-state facts" }
        val initialDefaults = specificDefaults.toList()
        val plans = candidates.map { candidate ->
            val common = candidate.common
            val commonParameters = common.cjmpParametersForMatching()
            val commonDefaults = candidate.commonDefaults.toList()
            require(commonDefaults.size == commonParameters.size) { "Every common parameter must have explicit default-state facts" }
            val facts = AbstractCjmpMatcher.prepareCandidate(specific.symbol, common.symbol, context, parentMapping)
            // 只有兼容签名才执行参数程序；参数中途失败不会撤销此前的默认来源转移。
            val parameters = if (specificParameters.size == commonParameters.size) {
                specificParameters.zip(commonParameters).mapIndexed { index, (specificParameter, commonParameter) ->
                    CjmpParameterMatch(
                        index = index,
                        namesMatch = specificParameter.isNamed == commonParameter.isNamed &&
                                (!specificParameter.isNamed || specificParameter.name == commonParameter.name),
                        commonDefault = commonDefaults[index],
                    )
                }
            } else {
                emptyList()
            }
            CjmpCandidatePlan(
                common = common.symbol as CfirBasedSymbol<*>,
                eligibility = facts.eligibility,
                beforeSignatureDiagnostics = candidate.beforeSignatureDiagnostics.toList(),
                signatureCompatibility = facts.signatureCompatibility,
                parameters = parameters,
                afterParameterDiagnostics = candidate.afterParameterDiagnostics.toList(),
                afterParameters = candidate.afterParameters,
                beforeBindingDiagnostics = candidate.beforeBindingDiagnostics.toList(),
                binding = bindingRule(specific, common,
                    CjmpBindingRequest<CfirBasedSymbol<*>, CfirTypeParameterSymbol>(
                        specific = specific.symbol,
                        common = common.symbol,
                        typeParameterMapping = facts.typeParameterMapping,
                        requiresImplementation = facts.requiresImplementation,
                    ),
                ),
            )
        }
        return CjmpMatchPlan.Declaration(
            specific = specific.symbol,
            initialDefaults = initialDefaults,
            candidates = plans,
            traversal = traversal,
            checkParameterDefaultValueConflict = session.checkParameterDefaultValueConflict(),
        )
    }

    /**
     * 官方分支由双方 native kind 决定：双方 enum 构造项才检查占用，mixed VAR 保留真实变量赋值规则。
     * nullary enum 本身没有 static 位，不能以 enum 身份绕过实例 seed 或静态性不一致分支。
     */
    private fun bindingRule(
        specific: CfirCallableDeclaration,
        common: CfirCallableDeclaration,
        request: CjmpBindingRequest<CfirBasedSymbol<*>, CfirTypeParameterSymbol>,
    ): CjmpBindingRule<CfirBasedSymbol<*>, CfirTypeParameterSymbol> {
        val bothEnums = specific is CfirEnumConstructor && common is CfirEnumConstructor
        val specificVariable = specific is CfirVariable || specific is CfirEnumConstructor && specific.valueParameters.isEmpty()
        val commonVariable = common is CfirVariable || common is CfirEnumConstructor && common.valueParameters.isEmpty()
        if (bothEnums || !specificVariable || !commonVariable) return CjmpBindingRule.Callable(request)
        val variableRequest = request.copy(requiresImplementation = false)
        return when {
            specific.status.isStatic || specific.getContainingCjmpDeclaration() == null ->
                CjmpBindingRule.VariableAssignment(variableRequest, CjmpVariableAssignmentKind.STATIC_OR_GLOBAL)
            specific.status.isStatic != common.status.isStatic ->
                CjmpBindingRule.VariableAssignment(variableRequest, CjmpVariableAssignmentKind.STATIC_MISMATCH)
            else -> CjmpBindingRule.MergedMemberVariable(variableRequest)
        }
    }

    /** 规范组发现只供显式聚合消费者使用，单目标 CJMP 不推进这些兄弟。 */
    fun findSpecificDeclarationsInMatchOrder(
        specific: CfirMemberDeclaration,
        session: CfirSession,
    ): List<CfirMemberDeclaration> = when (specific) {
        is CfirPatternVariable -> findSpecificTopLevelPatternVariablesInMatchOrder(specific, session)
        is CfirCallableDeclaration -> findSpecificCallablesInMatchOrder(specific, session)
        else -> listOf(specific)
    }

    /**
     * 按声明真实 owner 查找匹配候选，供匹配与前置 common 签名准备共同使用。
     * 父容器没有成功配对时，不从同名类或顶层符号恢复成员候选。
     */
    fun findCommonCandidatesForDeclaration(
        specific: CfirDeclaration,
        session: CfirSession,
        preparedOwnerFacts: CfirCjmpDeclarationMatchFacts? = null,
    ): List<CfirDeclaration> = prepareCommonCandidateDeclarations(
        specific,
        session,
        collectCommonCandidateDeclarations(specific, session, preparedOwnerFacts),
    )

    /**
     * 在目标声明读锁内只收集候选声明引用；阶段推进与 extend key 比较留给锁外准备。
     */
    fun collectCommonCandidateDeclarations(
        specific: CfirDeclaration,
        session: CfirSession,
        preparedOwnerFacts: CfirCjmpDeclarationMatchFacts? = null,
    ): List<CfirDeclaration> {
        if (!specific.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return emptyList()
        if (specific is CfirCallableDeclaration) {
            val owner = specific.getContainingCjmpDeclaration()
            if (owner != null) {
                val facts = preparedOwnerFacts?.takeIf { it.rootSymbol === owner.symbol }
                    ?: checkNotNull(owner.cjmpMatchFacts) { "A member requires the independent facts of its physical owner" }
                return collectCommonMemberCandidateDeclarations(specific, facts.commonContainers.map { it.symbol.cfir })
            }
        }
        return collectCommonCandidates(specific, session)
    }

    /**
     * 在候选引用离开目标读锁后完成签名阶段和 extend key 筛选。
     */
    fun prepareCommonCandidateDeclarations(
        specific: CfirDeclaration,
        session: CfirSession,
        candidates: List<CfirDeclaration>,
    ): List<CfirDeclaration> {
        val dependencyModules = specific.moduleData.allRefinementDependencies.toSet()
        val commonCandidates = candidates.distinct().filter { candidate ->
            candidate.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON) && candidate.moduleData in dependencyModules
        }
        if (specific !is CfirExtend) return commonCandidates

        specific.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val packageName = session.extendProvider.getPackageFqName(specific) ?: return emptyList()
        return commonCandidates.filterIsInstance<CfirExtend>()
            .filter { session.extendProvider.getPackageFqName(it) == packageName }
            .onEach { it.lazyResolveToPhase(CfirResolvePhase.TYPES) }
            .filter { candidate ->
                candidate !== specific &&
                        CfirCjmpMatcher.haveSameExtendKey(specific, candidate) == CjmpTypeCompatibility.COMPATIBLE
            }
    }

    /**
     * 返回 specific callable 的 first-fit 匹配组。
     *
     * 顶层声明跨文件按官方文件名与源码位置稳定排序；class/extend 成员保持所属声明列表的源码顺序，
     * 以便 eager 遍历和 LL 对单个成员的并发请求得到相同的绑定胜者。
     */
    fun findSpecificCallablesInMatchOrder(
        specific: CfirCallableDeclaration,
        session: CfirSession,
    ): List<CfirCallableDeclaration> {
        if (!specific.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return listOf(specific)

        val specificContainer = specific.getContainingCjmpDeclaration()
        val declarations = when (specificContainer) {
            is CfirClassLikeDeclaration -> specificContainer.declarations
            is CfirExtend -> specificContainer.declarations
            else -> null
        }
        if (declarations != null) {
            val matchingMembers = declarations
                .filterIsInstance<CfirCallableDeclaration>()
                .filter { candidate ->
                    candidate.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC) && candidate.symbol.name == specific.symbol.name
                }
            return (matchingMembers + specific).distinctBy { it.symbol }
        }
        return findSpecificTopLevelCallablesInMatchOrder(specific, session)
    }

    /**
     * 返回顶层 specific callable 的完整同名匹配组，并按官方文件名与声明顺序排列。
     *
     * LL 按目标懒解析时也必须先完成该组，才能保持 cjc `MatchCJMPDecls` 的 first-fit 绑定次序。
     */
    fun findSpecificTopLevelCallablesInMatchOrder(
        specific: CfirCallableDeclaration,
        session: CfirSession,
    ): List<CfirCallableDeclaration> {
        if (!specific.status.isSpecific) return listOf(specific)
        val callableId = specific.symbol.callableId
        if (callableId.classId != null || specific.isLocal || session.extendProvider.getContainingExtend(specific.symbol) != null) {
            return listOf(specific)
        }

        val siblings = session.symbolProvider
            .getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
            .asSequence()
            .map { it.cfir }
            .filterIsInstance<CfirCallableDeclaration>()
            .filter { candidate ->
                candidate.moduleData == specific.moduleData &&
                        candidate.status.isSpecific &&
                        candidate.symbol.callableId == callableId &&
                        !candidate.isLocal &&
                        session.extendProvider.getContainingExtend(candidate.symbol) == null
            }
            .toList()
        return (listOf(specific) + siblings)
            .distinctBy { it.symbol }
            .sortedWith(
                compareBy<CfirCallableDeclaration>(
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.name.orEmpty() },
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.path.orEmpty() },
                    { it.source?.startOffset ?: Int.MAX_VALUE },
                    { it.source?.endOffset ?: Int.MAX_VALUE },
                ),
            )
    }

    /** 同名 pattern binding 也共享官方 specific 声明 first-fit 顺序。 */
    fun findSpecificTopLevelPatternVariablesInMatchOrder(
        specific: CfirPatternVariable,
        session: CfirSession,
    ): List<CfirPatternVariable> {
        if (!specific.status.isSpecific || specific.symbol.callableId.classId != null || specific.isLocal ||
            session.extendProvider.getContainingExtend(specific.symbol) != null
        ) {
            return listOf(specific)
        }

        val bindingNames = specific.pattern.cjmpBindingVariables().mapTo(linkedSetOf()) { it.name }
        val candidates = bindingNames.flatMap { bindingName ->
            session.symbolProvider
                .getTopLevelCallableSymbols(specific.symbol.callableId.packageName, bindingName)
                .map { it.cfir }
                .mapNotNull { declaration ->
                    when (declaration) {
                        is CfirPatternVariable -> declaration
                        is CfirPatternBindingVariable -> session.cfirProvider.getCfirPatternVariableForBinding(declaration.symbol)
                        else -> null
                    }
                }
        }.filter { candidate ->
            candidate.moduleData == specific.moduleData &&
                    candidate.status.isSpecific &&
                    candidate.symbol.callableId.classId == null &&
                    !candidate.isLocal &&
                    session.extendProvider.getContainingExtend(candidate.symbol) == null &&
                    candidate.pattern.cjmpBindingVariables().any { it.name in bindingNames }
        }

        return (listOf(specific) + candidates)
            .distinctBy { it.symbol }
            .sortedWith(
                compareBy<CfirPatternVariable>(
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.name.orEmpty() },
                    { session.cfirProvider.getContainingFile(it.symbol)?.sourceFile?.path.orEmpty() },
                    { it.source?.startOffset ?: Int.MAX_VALUE },
                    { it.source?.endOffset ?: Int.MAX_VALUE },
                ),
            )
    }

    /**
     * 求 [specific] 声明的 common 候选（结构匹配前的候选集合，未做兼容性判定）。
     */
    fun findCommonCandidates(specific: CfirDeclaration, session: CfirSession): List<CfirDeclaration> {
        return prepareCommonCandidateDeclarations(
            specific,
            session,
            collectCommonCandidateDeclarations(specific, session),
        )
    }

    /** 收集未经阶段推进和结构兼容性筛选的原始依赖声明。 */
    private fun collectCommonCandidates(specific: CfirDeclaration, session: CfirSession): List<CfirDeclaration> {
        val dependencyModules = specific.moduleData.allRefinementDependencies
        if (dependencyModules.isEmpty()) return emptyList()
        val provider = session.dependenciesSymbolProvider

        val rawCandidates: List<CfirDeclaration> = when (specific) {
            is CfirClassLikeDeclaration ->
                provider.getClassLikeSymbolsByClassId(specific.symbol.classId).map { it.cfir }

            is org.cangnova.cangjie.cfir.declarations.CfirExtend -> {
                val targetKey = specific.extendedTypeRef.coneTypeOrNull?.expandedExtendTargetKey
                    ?: return emptyList()
                val packageName = session.extendProvider.getPackageFqName(specific) ?: return emptyList()
                session.extendProvider.getExtendsForTarget(targetKey, packageName)
            }

            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> {
                // 模式变量以绑定名参与顶层查找（`let v: T = e` / 元组模式逐变量），对位官方 VarDecl / VarWithPatternDecl
                val packageName = specific.symbol.callableId.packageName
                specific.pattern.cjmpBindingVariables().flatMap { binding ->
                    provider.getTopLevelCallableSymbols(packageName, binding.name).map { it.cfir }
                }
            }

            is CfirCallableDeclaration -> {
                val callableId = specific.symbol.callableId
                check(callableId.classId == null) {
                    "Member common candidates must be queried through the matched containing declaration"
                }
                provider.getTopLevelCallableSymbols(callableId.packageName, callableId.callableName)
                    .map { it.cfir }
            }

            else -> emptyList()
        }

        return rawCandidates.distinct()
    }

    /**
     * 在已配对的 nominal 或 extend 容器中查找成员候选。
     *
     * 该入口由成员自己的 CJMP_MATCHING 调用使用；common 容器必须来自 specific 父声明的已存储配对，
     * 以保证外层匹配失败时不会独立配对其子成员。
     */
    fun findCommonMemberCandidates(
        specific: CfirCallableDeclaration,
        commonContainer: CfirDeclaration,
    ): List<CfirCallableDeclaration> = findCommonMemberCandidates(specific, listOf(commonContainer))

    /** 同 key 的多个 common extend 都贡献其直接成员候选。 */
    fun findCommonMemberCandidates(
        specific: CfirCallableDeclaration,
        commonContainers: List<CfirDeclaration>,
    ): List<CfirCallableDeclaration> = collectCommonMemberCandidateDeclarations(specific, commonContainers)
        .filter { it.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.COMMON) }

    /** 在 owner 的稳定 common 容器中只按名字收集原始 callable 引用。 */
    private fun collectCommonMemberCandidateDeclarations(
        specific: CfirCallableDeclaration,
        commonContainers: List<CfirDeclaration>,
    ): List<CfirCallableDeclaration> = commonContainers.flatMap { commonContainer ->
        val commonDeclarations = when (commonContainer) {
            is CfirClassLikeDeclaration -> commonContainer.declarations
            is CfirExtend -> commonContainer.declarations
            else -> emptyList()
        }
        commonDeclarations.filterIsInstance<CfirCallableDeclaration>().filter { candidate ->
            candidate.symbol.name == specific.symbol.name
        }
    }.distinctBy { it.symbol }
}

/** 只收集 native VAR 的执行前缀，候选发现也不能越过 tuple 的停止项。 */
internal fun CfirPattern.cjmpBindingVariables(): List<CfirPatternBindingVariable> = when (this) {
    is CfirBindingPattern if nestedPattern == null -> listOf(requireNotNull(bindingVariable))
    is CfirTuplePattern -> elements.takeWhile { it is CfirBindingPattern && it.nestedPattern == null }
        .map { requireNotNull((it as CfirBindingPattern).bindingVariable) }
    else -> emptyList()
}

/**
 * property 候选在签名计算之外的检查事实；property 无参数，因此只携带对位声明与默认值事实。
 */
data class CfirCjmpPropertyCandidate(
    val common: CfirProperty,
)

/**
 * static/global 变量候选；变量无参数，只携带对位声明。
 */
data class CfirCjmpVariableCandidate(
    val common: CfirVariable,
)

/**
 * 该变量是否按官方 `MatchCJMPVar` 在 Match 阶段配对（global 或 static）。
 *
 * 判据镜像官方 `IsStaticOrGlobal()`：`static` 修饰，或顶层（无 containing class → classId 为空）。
 * 实例成员变量由容器合并配对，不属于本入口。
 */
fun CfirVariable.isCjmpStaticOrGlobalVariable(): Boolean =
    status.isStatic || getContainingCjmpDeclaration() == null

/**
 * 函数候选在签名计算之外的**有序检查事实**。每个字段都必须由调用方提供，保留默认来源、诊断与失败的先后边界。
 * 这里只携带数据，不保存 checker、reporter 或延迟执行的闭包。
 *
 * 资格与签名由共享 matcher 准备，参数程序只在求值器内执行。
 */
data class CfirCjmpCallableCandidate<Diagnostic>(
    val common: CfirCallableDeclaration,
    val commonDefaults: List<CjmpParameterDefaultState<CfirBasedSymbol<*>>>,
    val beforeSignatureDiagnostics: List<Diagnostic>,
    val afterParameterDiagnostics: List<Diagnostic>,
    val afterParameters: CjmpMatchResult,
    val beforeBindingDiagnostics: List<Diagnostic>,
)
