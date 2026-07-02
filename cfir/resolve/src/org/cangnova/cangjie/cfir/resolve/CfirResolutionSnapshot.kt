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

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolveState
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.declarations.hasLambdaParameterShapeDiagnostic
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.impl.CfirMatchBranchImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternMutableState
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import java.util.IdentityHashMap

/**
 * CFIR 树可变解析状态快照。
 *
 * lambda 重检、overload-by-lambda 和 PCLA completion 都可能在同一棵表达式树上多次执行 body resolve。
 * 快照负责恢复所有会被 body resolve 改写的类型、引用、body、pattern、CFG 和 resolve state，
 * 使后续重算从结构化 CFIR 状态出发，而不是继续消费上一轮的错误候选或 resolved reference。
 */
internal class CfirResolutionSnapshot private constructor(
    /** 带 resolve state 元素的阶段状态快照。 */
    private val resolveStates: IdentityHashMap<CfirElementWithResolveState, CfirResolveState>,
    /** 表达式到其 cone type 的快照。 */
    private val expressionTypes: IdentityHashMap<CfirExpression, ConeCangJieType?>,
    /** 可解析表达式到 callee reference 的快照。 */
    private val calleeReferences: IdentityHashMap<CfirResolvable, CfirReference>,
    /** 匿名函数返回类型引用快照。 */
    private val anonymousFunctionReturnTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
    /** 匿名函数整体函数类型引用快照。 */
    private val anonymousFunctionTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
    /** 匿名函数匹配参数函数类型快照。 */
    private val anonymousFunctionMatchingTypes: IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>,
    /** 匿名函数参数形状诊断标记快照。 */
    private val anonymousFunctionShapeDiagnostics: IdentityHashMap<CfirAnonymousFunction, Boolean?>,
    /** 匿名函数参数形状诊断目标函数类型快照。 */
    private val anonymousFunctionShapeExpectedTypes: IdentityHashMap<CfirAnonymousFunction, ConeFunctionType?>,
    /** 匿名函数 body 快照。 */
    private val anonymousFunctionBodies: IdentityHashMap<CfirAnonymousFunction, CfirBlock?>,
    /** 变量返回类型引用快照。 */
    private val variableTypes: IdentityHashMap<CfirVariable, CfirTypeRef>,
    /** block 语句列表快照。 */
    private val blockStatements: IdentityHashMap<CfirBlock, List<CfirStatement>>,
    /** 模式变量当前 pattern 快照。 */
    private val patternVariablePatterns: IdentityHashMap<CfirPatternVariable, CfirPattern>,
    /** match branch 当前 pattern 快照。 */
    private val matchBranchPatterns: IdentityHashMap<CfirMatchBranch, CfirPattern>,
    /** pattern 内部可变状态快照。 */
    private val patternStates: IdentityHashMap<CfirPattern, CfirPatternMutableState>,
    /** qualified access 接收者与类型实参快照。 */
    private val qualifiedAccessStates: IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>,
    /** 函数调用实参列表快照。 */
    private val functionCallArgumentLists: IdentityHashMap<CfirFunctionCall, CfirArgumentList>,
    /** CFG owner 到 CFG reference 的快照。 */
    private val controlFlowGraphReferences: IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>,
) {
    /**
     * 把快照中的全部 CFIR 可变状态恢复到原对象上。
     */
    @OptIn(ResolveStateAccess::class)
    fun restore() {
        for ((element, state) in resolveStates) {
            element.resolveState = state
        }
        for ((owner, reference) in controlFlowGraphReferences) {
            owner.replaceControlFlowGraphReference(reference)
        }
        for ((variable, typeRef) in variableTypes) {
            variable.replaceReturnTypeRef(typeRef)
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
        for ((function, hasShapeDiagnostic) in anonymousFunctionShapeDiagnostics) {
            function.hasLambdaParameterShapeDiagnostic = hasShapeDiagnostic
        }
        for ((function, expectedFunctionType) in anonymousFunctionShapeExpectedTypes) {
            function.lambdaParameterShapeExpectedFunctionType = expectedFunctionType
        }
        for ((function, body) in anonymousFunctionBodies) {
            function.replaceBody(body)
        }
        for ((patternVariable, pattern) in patternVariablePatterns) {
            val impl = patternVariable as? CfirPatternVariableImpl
                ?: error("CfirPatternVariable must be backed by generated implementation")
            impl.pattern = pattern
        }
        for ((branch, pattern) in matchBranchPatterns) {
            val impl = branch as? CfirMatchBranchImpl
                ?: error("CfirMatchBranch must be backed by generated implementation")
            impl.pattern = pattern
        }
        for ((_, state) in patternStates) {
            state.restore()
        }
        for ((block, statements) in blockStatements) {
            val mutableStatements = block.statements as? MutableList<CfirStatement>
                ?: error("CfirBlock statements must be mutable during body resolve snapshot restore")
            mutableStatements.clear()
            mutableStatements.addAll(statements)
        }
        for ((call, argumentList) in functionCallArgumentLists) {
            call.replaceArgumentList(argumentList)
        }
        for ((access, state) in qualifiedAccessStates) {
            access.replaceDispatchReceiver(state.dispatchReceiver)
            access.replaceTypeArguments(state.typeArguments)
        }
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
        /** 当前类型实参列表。 */
        val typeArguments: List<CfirTypeRef>,
    )

    companion object {
        /**
         * 从根元素遍历并捕获后续 body resolve 可恢复状态。
         */
        fun capture(root: CfirElement): CfirResolutionSnapshot {
            val resolveStates = IdentityHashMap<CfirElementWithResolveState, CfirResolveState>()
            val expressionTypes = IdentityHashMap<CfirExpression, ConeCangJieType?>()
            val calleeReferences = IdentityHashMap<CfirResolvable, CfirReference>()
            val anonymousFunctionReturnTypes = IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>()
            val anonymousFunctionTypes = IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>()
            val anonymousFunctionMatchingTypes = IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>()
            val anonymousFunctionShapeDiagnostics = IdentityHashMap<CfirAnonymousFunction, Boolean?>()
            val anonymousFunctionShapeExpectedTypes = IdentityHashMap<CfirAnonymousFunction, ConeFunctionType?>()
            val anonymousFunctionBodies = IdentityHashMap<CfirAnonymousFunction, CfirBlock?>()
            val variableTypes = IdentityHashMap<CfirVariable, CfirTypeRef>()
            val blockStatements = IdentityHashMap<CfirBlock, List<CfirStatement>>()
            val patternVariablePatterns = IdentityHashMap<CfirPatternVariable, CfirPattern>()
            val matchBranchPatterns = IdentityHashMap<CfirMatchBranch, CfirPattern>()
            val patternStates = IdentityHashMap<CfirPattern, CfirPatternMutableState>()
            val qualifiedAccessStates = IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>()
            val functionCallArgumentLists = IdentityHashMap<CfirFunctionCall, CfirArgumentList>()
            val controlFlowGraphReferences =
                IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>()

            root.accept(
                object : CfirVisitorVoid() {
                    /**
                     * 捕获当前元素及其子树中所有可回滚状态。
                     */
                    @OptIn(ResolveStateAccess::class)
                    override fun visitElement(element: CfirElement) {
                        if (element is CfirElementWithResolveState) {
                            resolveStates[element] = element.resolveState
                        }
                        if (element is CfirExpression && element !is CfirAnonymousFunctionExpression) {
                            expressionTypes[element] = element.coneTypeOrNull
                        }
                        if (element is CfirResolvable) {
                            calleeReferences[element] = element.calleeReference
                        }
                        if (element is CfirAnonymousFunction) {
                            anonymousFunctionReturnTypes[element] = element.returnTypeRef
                            anonymousFunctionTypes[element] = element.typeRef
                            anonymousFunctionMatchingTypes[element] = element.matchingParameterFunctionType
                            anonymousFunctionShapeDiagnostics[element] = element.hasLambdaParameterShapeDiagnostic
                            anonymousFunctionShapeExpectedTypes[element] = element.lambdaParameterShapeExpectedFunctionType
                            anonymousFunctionBodies[element] = element.body
                        }
                        if (element is CfirVariable) {
                            variableTypes[element] = element.returnTypeRef
                        }
                        if (element is CfirBlock) {
                            blockStatements[element] = element.statements.toList()
                        }
                        if (element is CfirPatternVariable) {
                            patternVariablePatterns[element] = element.pattern
                        }
                        if (element is CfirMatchBranch) {
                            matchBranchPatterns[element] = element.pattern
                        }
                        if (element is CfirPattern) {
                            CfirPatternMutableState.capture(element)?.let { patternState ->
                                patternStates[element] = patternState
                            }
                        }
                        if (element is CfirQualifiedAccessExpression) {
                            qualifiedAccessStates[element] = QualifiedAccessState(
                                dispatchReceiver = element.dispatchReceiver,
                                typeArguments = element.typeArguments.toList(),
                            )
                        }
                        if (element is CfirFunctionCall) {
                            functionCallArgumentLists[element] = element.argumentList
                        }
                        if (element is CfirControlFlowGraphOwner) {
                            controlFlowGraphReferences[element] = element.controlFlowGraphReference
                        }
                        element.acceptChildren(this, null)
                    }
                },
                null,
            )

            return CfirResolutionSnapshot(
                resolveStates,
                expressionTypes,
                calleeReferences,
                anonymousFunctionReturnTypes,
                anonymousFunctionTypes,
                anonymousFunctionMatchingTypes,
                anonymousFunctionShapeDiagnostics,
                anonymousFunctionShapeExpectedTypes,
                anonymousFunctionBodies,
                variableTypes,
                blockStatements,
                patternVariablePatterns,
                matchBranchPatterns,
                patternStates,
                qualifiedAccessStates,
                functionCallArgumentLists,
                controlFlowGraphReferences,
            )
        }
    }
}
