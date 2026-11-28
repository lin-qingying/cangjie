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
 * distributed under the License is distributed on an "AS IS", BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResourceVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolveState
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.declarations.isInsideFailedArgumentMapping
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirArgumentListMutableState
import org.cangnova.cangjie.cfir.expressions.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternMutableState
import org.cangnova.cangjie.cfir.patterns.CfirPatternTreeOwner
import org.cangnova.cangjie.cfir.patterns.restorePatternTreeRoots
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirResourceSourceSnapshot
import org.cangnova.cangjie.cfir.resolve.body.captureResourceSourceStates
import org.cangnova.cangjie.cfir.resolve.body.restoreResourceSourceStates
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.RevisableFunctionCallNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.RevisableFunctionCallNodeSnapshot
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression

/**
 * CFIR 树可变解析状态快照。
 *
 * lambda 重检、overload-by-lambda 和 PCLA completion 都可能在同一棵表达式树上多次执行 body resolve。
 * 快照负责恢复所有会被 body resolve 改写的类型、引用、body、pattern、CFG 和 resolve state，
 * 使后续重算从结构化 CFIR 状态出发，而不是继续消费上一轮的错误候选或 resolved reference。
 */
class CfirResolutionSnapshot private constructor(
    /** 原 AST 根用于确定试跑新建、即将被恢复移除的子声明；不沿 resolved symbol 访问外部声明。 */
    private val root: CfirElement,
    /** 带 resolve state 元素的阶段状态快照。 */
    private val resolveStates: IdentityHashMap<CfirElementWithResolveState, CfirResolveState>,
    /** 只捕获可写的表达式类型；代理到子节点或诊断的派生类型由其真实 owner 恢复。 */
    private val expressionTypes: IdentityHashMap<CfirExpression, ConeCangJieType?>,
    /** 可解析表达式到 callee reference 的快照。 */
    private val calleeReferences: IdentityHashMap<CfirResolvable, CfirReference>,
    /** 匿名函数返回类型引用快照。 */
    private val anonymousFunctionReturnTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
    /** 匿名函数整体函数类型引用快照。 */
    private val anonymousFunctionTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
    /** 匿名函数匹配参数函数类型快照。 */
    private val anonymousFunctionMatchingTypes: IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>,
    /** 匿名函数参数形状诊断目标函数类型快照。 */
    private val anonymousFunctionShapeExpectedTypes: IdentityHashMap<CfirAnonymousFunction, ConeFunctionType?>,
    /** 尚未分析的 lambda 是否位于参数映射失败的调用中。 */
    private val anonymousFunctionArgumentMappingFailures: IdentityHashMap<CfirAnonymousFunction, Boolean>,
    /** 匿名函数 body 快照。 */
    private val anonymousFunctionBodies: IdentityHashMap<CfirAnonymousFunction, CfirBlock?>,
    /** 变量返回类型引用快照。 */
    private val variableTypes: IdentityHashMap<CfirVariable, CfirTypeRef>,
    /** 与资源 source/cache/回调队列同代次；source 自持的 initializer memento 不再递归持有该列表。 */
    private val resourceSources: List<CfirResourceSourceSnapshot>?,
    /** 只保存真正拥有 initializer 的声明槽，模式叶子的派生 initializer 不参与写回。 */
    private val variableInitializers: IdentityHashMap<CfirVariable, CfirExpression?>,
    /** block 语句列表快照。 */
    private val blockStatements: IdentityHashMap<CfirBlock, List<CfirStatement>>,
    /** 资源声明列表可在试算中物化；先恢复列表形状，再按正式 child 协议恢复槽位。 */
    private val tryResources: IdentityHashMap<CfirTryExpression, List<CfirResourceVariable>>,
    /** 所有模式宿主当前根节点快照，覆盖声明、表达式、match、catch 与 handle。 */
    private val patternTreeRoots: List<Pair<CfirPatternTreeOwner, CfirElement>>,
    /** pattern 内部可变状态快照。 */
    private val patternStates: IdentityHashMap<CfirPattern, CfirPatternMutableState>,
    /** 下标语法节点与已解析 get/set 调用的关联必须随候选试跑一起回滚。 */
    private val subscriptCallStates: IdentityHashMap<CfirSubscriptExpression, SubscriptCallState>,
    /** qualified access 接收者与类型实参快照。 */
    private val qualifiedAccessStates: IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>,
    /** 函数调用实参列表快照。 */
    private val functionCallArgumentLists: IdentityHashMap<CfirFunctionCall, CfirArgumentList>,
    /** 实参列表元素与形参映射的独立存储快照。 */
    private val argumentListStates: IdentityHashMap<CfirArgumentList, CfirArgumentListMutableState>,
    /** 由正式 child-transform 协议记录的直接子节点槽；覆盖 wrapper、literal 容器和嵌套声明。 */
    private val childSlots: List<ChildSlots>,
    /** CFG owner 到 CFG reference 的快照。 */
    private val controlFlowGraphReferences: IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>,
    /** 完整 AST 回滚保存实际 fir/index 状态；类型重放保留正在被遍历的当前 AST/CFG。 */
    private val callNodeFirStates: List<RevisableFunctionCallNodeSnapshot>,
) {
    internal val callableSymbols: Set<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>
        get() = resolveStates.keys.filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration>()
            .mapTo(linkedSetOf()) { it.symbol }

    /** 缓存范围直接来自同一 AST 捕获，不按 locality、名称或 offset 猜测。 */
    internal fun captureImplicitBodyCache(owner: org.cangnova.cangjie.cfir.resolve.body.ImplicitBodyResolveComputationSession?):
        org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSessionSnapshot? = owner?.capture(callableSymbols)

    /** AST 和其 scoped cache 同步重放；先验证 owner/父栈，再改写任一状态。 */
    internal fun restoreWithImplicitBodyCache(
        cache: org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSessionSnapshot?,
        owner: org.cangnova.cangjie.cfir.resolve.body.ImplicitBodyResolveComputationSession?,
    ) {
        check((cache == null) == (owner == null)) { "Implicit body cache snapshot/owner mismatch" }
        if (cache != null) {
            check(cache.symbols == callableSymbols) { "Implicit body cache/AST scope mismatch" }
            checkNotNull(owner).validateRestore(cache)
        }
        // lazy body 首轮物化可能创建全新的局部 symbols，恢复旧 body 前必须把它们纳入失效范围。
        val affectedSymbols = if (cache == null) emptySet() else callableSymbols + capture(root).callableSymbols
        restore()
        if (cache != null) checkNotNull(owner).restore(cache, affectedSymbols)
    }

    /** 终选采用同时发布实际 CFG read dependency；probe/类型重放不取得这份正式图资格。 */
    @OptIn(CfgInternals::class)
    internal fun restoreForSelectedCandidate(
        cache: org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSessionSnapshot?,
        cacheOwner: org.cangnova.cangjie.cfir.resolve.body.ImplicitBodyResolveComputationSession?,
        dataFlowContext: org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext,
    ) {
        val graphs = controlFlowGraphReferences.mapNotNull { (owner, reference) ->
            reference?.controlFlowGraph?.let { owner to it }
        }
        dataFlowContext.graphBuilder.validateControlFlowGraphAdoption(graphs)
        restoreWithImplicitBodyCache(cache, cacheOwner)
        dataFlowContext.graphBuilder.adoptControlFlowGraphs(graphs)
    }

    /**
     * 恢复类型推断写回的语义槽，保留正在被调用者遍历的 body、实参和普通 child 存储。
     * PCLA 入口之外的声明只会被类型/模式投影回调改写，不能对其执行整树重放。
     */
    @OptIn(ResolveStateAccess::class)
    fun restoreTypeResolutionState() {
        resourceSources?.forEach { it.validateRestore() }
        resourceSources?.forEach { it.restoreTypeResolutionState() }
        resolveStates.forEach { (element, state) -> element.resolveState = state }
        variableTypes.forEach { (variable, type) -> variable.replaceReturnTypeRef(type) }
        anonymousFunctionReturnTypes.forEach { (function, type) -> function.replaceReturnTypeRef(type) }
        anonymousFunctionTypes.forEach { (function, type) -> function.replaceTypeRef(type) }
        anonymousFunctionMatchingTypes.forEach { (function, type) -> function.replaceMatchingParameterFunctionType(type) }
        anonymousFunctionShapeExpectedTypes.forEach { (function, type) -> function.lambdaParameterShapeExpectedFunctionType = type }
        anonymousFunctionArgumentMappingFailures.forEach { (function, failed) -> function.isInsideFailedArgumentMapping = failed }
        restorePatternTreeRoots(patternTreeRoots)
        patternStates.values.forEach { it.restore() }
        calleeReferences.forEach { (expression, reference) -> expression.replaceCalleeReference(reference) }
        expressionTypes.forEach { (expression, type) -> expression.replaceConeTypeOrNull(type) }
    }

    /**
     * 把快照中的全部 CFIR 可变状态恢复到原对象上。
     */
    @OptIn(ResolveStateAccess::class, CfgInternals::class)
    fun restore() {
        callNodeFirStates.forEach { it.validateRestore() }
        resourceSources?.let { restoreResourceSourceStates(root, it) }
        callNodeFirStates.forEach { it.restore() }
        for ((element, state) in resolveStates) {
            element.resolveState = state
        }
        for ((owner, reference) in controlFlowGraphReferences) {
            owner.replaceControlFlowGraphReference(reference)
        }
        for ((variable, typeRef) in variableTypes) {
            variable.replaceReturnTypeRef(typeRef)
        }
        for ((variable, initializer) in variableInitializers) {
            if (variable is CfirPatternBindingVariable) variable.replaceOwnedInitializer(initializer)
            else variable.replaceInitializer(initializer)
        }
        for ((function, typeRef) in anonymousFunctionReturnTypes) {
            function.replaceReturnTypeRef(typeRef)
        }
        for ((function, typeRef) in anonymousFunctionTypes) {
            function.replaceTypeRef(typeRef)
        }
        for ((function, matchingType) in anonymousFunctionMatchingTypes) {
            function.replaceMatchingParameterFunctionType(matchingType)
        }
        for ((function, expectedFunctionType) in anonymousFunctionShapeExpectedTypes) {
            function.lambdaParameterShapeExpectedFunctionType = expectedFunctionType
        }
        for ((function, failed) in anonymousFunctionArgumentMappingFailures) {
            function.isInsideFailedArgumentMapping = failed
        }
        for ((function, body) in anonymousFunctionBodies) {
            function.replaceBody(body)
        }
        restorePatternTreeRoots(patternTreeRoots)
        for ((_, state) in patternStates) {
            state.restore()
        }
        for ((subscript, state) in subscriptCallStates) {
            subscript.replaceResolvedGetCall(state.getCall)
            subscript.replaceResolvedSetCall(state.setCall)
        }
        for ((block, statements) in blockStatements) block.replaceStatements(statements)
        for ((expression, resources) in tryResources) expression.replaceResources(resources)
        for ((call, argumentList) in functionCallArgumentLists) {
            call.replaceArgumentList(argumentList)
        }
        for ((_, state) in argumentListStates) state.restore()
        for ((access, state) in qualifiedAccessStates) {
            access.replaceDispatchReceiver(state.dispatchReceiver)
            access.replaceExplicitReceiver(state.explicitReceiver)
            access.replaceTypeArguments(state.typeArguments)
        }
        // 先恢复会改变子节点数量或 nullable/receiver alias 形状的独立存储，再恢复普通 child 槽。
        // acceptChildren 与 transformChildren 的 receiver 去重和访问顺序不同，不能混用两套协议。
        childSlots.forEach { it.restore() }
        for ((resolvable, reference) in calleeReferences) {
            resolvable.replaceCalleeReference(reference)
        }
        for ((expression, type) in expressionTypes) {
            expression.replaceConeTypeOrNull(type)
        }
    }

    /**
     * qualified access 的可变状态。
     */
    private data class QualifiedAccessState(
        /** 当前 dispatch receiver。 */
        val dispatchReceiver: CfirExpression?,
        /** 解析可能替换原始显式接收者表达式。 */
        val explicitReceiver: CfirExpression?,
        /** 当前类型实参列表。 */
        val typeArguments: List<CfirTypeRef>,
    )

    private data class SubscriptCallState(
        val getCall: CfirFunctionCall?,
        val setCall: CfirFunctionCall?,
    )

    /** 子树转换只替换当前槽，绝不在捕获或恢复时解析该子树。 */
    private class ChildSlots(val owner: CfirElement, val children: List<CfirElement>) {
        fun restore() {
            val iterator = children.iterator()
            owner.transformChildren(ChildSlotRestoringTransformer, iterator)
            check(!iterator.hasNext()) { "Unrestored child slots in ${owner.javaClass.simpleName}" }
        }
    }

    private object ChildSlotCollectingTransformer : CfirTransformer<MutableList<CfirElement>>() {
        override fun <E : CfirElement> transformElement(element: E, data: MutableList<CfirElement>): E {
            data += element
            return element
        }
    }

    private object ChildSlotRestoringTransformer : CfirTransformer<Iterator<CfirElement>>() {
        @Suppress("UNCHECKED_CAST")
        override fun <E : CfirElement> transformElement(element: E, data: Iterator<CfirElement>): E = data.next() as E
    }

    companion object {
        /**
         * 从根元素遍历并捕获后续 body resolve 可恢复状态。
         */
        fun capture(root: CfirElement): CfirResolutionSnapshot = captureState(
            root, captureResourceSourceStates(root),
        )

        /** Source slot 自己保存 cache/task；initializer 的标准 AST 状态不递归捕获同一协议。 */
        internal fun captureResourceInitializer(root: CfirElement): CfirResolutionSnapshot = captureState(root, null)

        private fun captureState(
            root: CfirElement,
            resourceSources: List<CfirResourceSourceSnapshot>?,
        ): CfirResolutionSnapshot {
            val resolveStates = IdentityHashMap<CfirElementWithResolveState, CfirResolveState>()
            val expressionTypes = IdentityHashMap<CfirExpression, ConeCangJieType?>()
            val calleeReferences = IdentityHashMap<CfirResolvable, CfirReference>()
            val anonymousFunctionReturnTypes = IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>()
            val anonymousFunctionTypes = IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>()
            val anonymousFunctionMatchingTypes = IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>()
            val anonymousFunctionShapeExpectedTypes = IdentityHashMap<CfirAnonymousFunction, ConeFunctionType?>()
            val anonymousFunctionArgumentMappingFailures = IdentityHashMap<CfirAnonymousFunction, Boolean>()
            val anonymousFunctionBodies = IdentityHashMap<CfirAnonymousFunction, CfirBlock?>()
            val variableTypes = IdentityHashMap<CfirVariable, CfirTypeRef>()
            val variableInitializers = IdentityHashMap<CfirVariable, CfirExpression?>()
            val blockStatements = IdentityHashMap<CfirBlock, List<CfirStatement>>()
            val tryResources = IdentityHashMap<CfirTryExpression, List<CfirResourceVariable>>()
            val patternTreeRoots = mutableListOf<Pair<CfirPatternTreeOwner, CfirElement>>()
            val patternStates = IdentityHashMap<CfirPattern, CfirPatternMutableState>()
            val subscriptCallStates = IdentityHashMap<CfirSubscriptExpression, SubscriptCallState>()
            // set 的 RHS 可以引用同一个 get 节点；语义调用引用因此形成共享图而非纯树。
            val visitedElements = java.util.Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
            val qualifiedAccessStates = IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>()
            val functionCallArgumentLists = IdentityHashMap<CfirFunctionCall, CfirArgumentList>()
            val argumentListStates = IdentityHashMap<CfirArgumentList, CfirArgumentListMutableState>()
            val childSlots = mutableListOf<ChildSlots>()
            val callNodeFirStates = mutableListOf<RevisableFunctionCallNodeSnapshot>()
            val capturedGraphs = java.util.Collections.newSetFromMap(IdentityHashMap<ControlFlowGraph, Boolean>())
            @OptIn(CfgInternals::class)
            fun captureCallNodeFirs(graph: ControlFlowGraph) {
                if (!capturedGraphs.add(graph)) return
                graph.nodes.filterIsInstance<RevisableFunctionCallNode>().forEach { callNodeFirStates += it.captureFirState() }
                graph.subGraphs.forEach(::captureCallNodeFirs)
            }
            val controlFlowGraphReferences =
                IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>()

            root.accept(
                object : CfirVisitorVoid() {
                    /**
                     * 捕获当前元素及其子树中所有可回滚状态。
                     */
                    @OptIn(ResolveStateAccess::class)
                    override fun visitElement(element: CfirElement) {
                        if (!visitedElements.add(element)) return
                        // 惰性节点只标记尚未物化的槽位，没有可读取的类型或子结构。
                        // 拥有该槽的声明/块保存节点引用，不能为捕获快照而提前物化正文。
                        if (element is CfirLazyBlock || element is CfirLazyExpression) return
                        if (element is CfirElementWithResolveState) {
                            resolveStates[element] = element.resolveState
                        }
                        // error/anonymous-function expression 的类型是派生只读属性。
                        // 错误节点每次读取还会生成新的诊断包装，不能把读出的对象当成可写字段恢复；
                        // 仍继续访问子树，使真正承载类型的子表达式或匿名函数参与快照。
                        if (element is CfirExpression &&
                            element !is CfirAnonymousFunctionExpression && element !is CfirErrorExpression
                        ) {
                            expressionTypes[element] = element.coneTypeOrNull
                        }
                        if (element is CfirResolvable) {
                            calleeReferences[element] = element.calleeReference
                        }
                        if (element is CfirAnonymousFunction) {
                            anonymousFunctionReturnTypes[element] = element.returnTypeRef
                            anonymousFunctionTypes[element] = element.typeRef
                            anonymousFunctionMatchingTypes[element] = element.matchingParameterFunctionType
                            anonymousFunctionShapeExpectedTypes[element] = element.lambdaParameterShapeExpectedFunctionType
                            anonymousFunctionArgumentMappingFailures[element] = element.isInsideFailedArgumentMapping
                            anonymousFunctionBodies[element] = element.body
                        }
                        if (element is CfirVariable) {
                            variableTypes[element] = element.returnTypeRef
                            when (element) {
                                is CfirPatternBindingVariable ->
                                    if (element.contentSource === CfirContentSource.Owned) {
                                        variableInitializers[element] = element.ownedInitializer
                                    }
                                is CfirFieldVariable, is CfirPatternVariable, is CfirResourceVariable ->
                                    variableInitializers[element] = element.initializer
                            }
                        }
                        if (element is CfirArgumentList) {
                            argumentListStates[element] = CfirArgumentListMutableState.capture(element)
                            if (element is CfirResolvedArgumentList) {
                                // 原始列表与 context 实参不是 arguments 视图的完整子树。
                                element.originalArgumentList?.accept(this, null)
                                element.mappingIncludingContextArguments.keys.forEach { it.accept(this, null) }
                            }
                        }
                        if (element is CfirBlock) {
                            blockStatements[element] = element.statements.toList()
                        }
                        if (element is CfirTryExpression) tryResources[element] = element.resources.toList()
                        if (element is CfirPatternTreeOwner) {
                            patternTreeRoots += element to element.patternTreeRoot()
                        }
                        if (element is CfirPattern) {
                            patternStates[element] = CfirPatternMutableState.capture(element)
                        }
                        if (element is CfirSubscriptExpression) {
                            subscriptCallStates[element] = SubscriptCallState(element.resolvedGetCall, element.resolvedSetCall)
                            element.resolvedGetCall?.accept(this, null)
                            element.resolvedSetCall?.accept(this, null)
                        }
                        if (element is CfirQualifiedAccessExpression) {
                            qualifiedAccessStates[element] = QualifiedAccessState(
                                dispatchReceiver = element.dispatchReceiver,
                                explicitReceiver = element.explicitReceiver,
                                typeArguments = element.typeArguments.toList(),
                            )
                        }
                        if (element is CfirFunctionCall) {
                            functionCallArgumentLists[element] = element.argumentList
                        }
                        if (element is CfirControlFlowGraphOwner) {
                            controlFlowGraphReferences[element] = element.controlFlowGraphReference
                            element.controlFlowGraphReference?.controlFlowGraph?.let(::captureCallNodeFirs)
                        }
                        // 模式图由 ownership-aware 状态恢复，实参列表由其独立存储协议恢复。
                        // 其他元素使用自身的正式 transformChildren，完整记录可以被转换替换的直接槽。
                        if (element !is CfirPattern && element !is CfirArgumentList) {
                            val children = mutableListOf<CfirElement>()
                            element.transformChildren(ChildSlotCollectingTransformer, children)
                            childSlots += ChildSlots(element, children)
                        }
                        element.acceptChildren(this, null)
                    }
                },
                null,
            )

            return CfirResolutionSnapshot(
                root,
                resolveStates,
                expressionTypes,
                calleeReferences,
                anonymousFunctionReturnTypes,
                anonymousFunctionTypes,
                anonymousFunctionMatchingTypes,
                anonymousFunctionShapeExpectedTypes,
                anonymousFunctionArgumentMappingFailures,
                anonymousFunctionBodies,
                variableTypes,
                resourceSources,
                variableInitializers,
                blockStatements,
                tryResources,
                patternTreeRoots,
                patternStates,
                subscriptCallStates,
                qualifiedAccessStates,
                functionCallArgumentLists,
                argumentListStates,
                childSlots,
                controlFlowGraphReferences,
                callNodeFirStates,
            )
        }
    }
}
