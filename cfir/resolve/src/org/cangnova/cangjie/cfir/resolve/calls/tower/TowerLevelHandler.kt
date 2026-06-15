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
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.cangnova.cangjie.cfir.calls.ExpressionReceiverValue
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.calls.resolvedQualifierSymbol
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.ConstructorFilter
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.isInstanceExtendMemberCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.processFunctionsAndConstructorsByName
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

internal data class CandidateFactoriesAndCollectors(
    val candidateFactory: CandidateFactory,
    val resultCollector: CfirCandidateCollector,
)

internal enum class ProcessResult {
    SCOPE_EMPTY,
    FOUND,
    ;

    operator fun plus(other: ProcessResult): ProcessResult =
        if (this == FOUND || other == FOUND) FOUND else SCOPE_EMPTY
}

internal interface CfirTowerLevel {
    fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult
    fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult
}

internal class ScopeBasedTowerLevel(
    private val components: BodyResolveComponents,
    private val scope: CfirScope,
    private val dispatchReceiver: ReceiverValue? = null,
    private val givenExtensionReceiver: ReceiverValue? = null,
) : CfirTowerLevel {
    /**
     * 对齐 Kotlin `TowerLevels.consumeCallableCandidate`：
     * candidate 在进入 argument mapping / checking 之前，先推进到 TYPES。
     */
    private fun consumeCallableCandidate(
        symbol: CfirCallableSymbol<*>,
        processor: TowerLevelProcessor,
    ) {
        symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        if (givenExtensionReceiver != null && !symbol.isInstanceExtendMemberCandidate(components.session)) {
            return
        }
        processor.consumeCandidate(symbol, scope, dispatchReceiver, givenExtensionReceiver)
    }

    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        var result = ProcessResult.SCOPE_EMPTY
        scope.processCallablesByName(info.name) { symbol ->
            // 仓颉 enum constructor 在普通名字访问中不是普通 callable。
            // 裸 enum 值访问由 CfirCallResolver 的 EnumConstructorCall fallback 单独进入，
            // 否则同名顶层变量/函数会被错误地与 enum constructor 合并成歧义。
            if (info.callKind == CallKind.NamedValueAccess && symbol is CfirEnumConstructorSymbol) {
                return@processCallablesByName
            }
            result = ProcessResult.FOUND
            consumeCallableCandidate(symbol, processor)
        }
        return result
    }

    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (info.callKind == CallKind.EnumConstructorCall) {
            var result = ProcessResult.SCOPE_EMPTY
            scope.processCallablesByName(info.name) { symbol ->
                val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return@processCallablesByName
                result = ProcessResult.FOUND
                consumeCallableCandidate(enumConstructorSymbol, processor)
            }
            return result
        }

        var result = ProcessResult.SCOPE_EMPTY
        val constructorFilter = when (givenExtensionReceiver) {
            null -> ConstructorFilter.OnlyNested
            else -> ConstructorFilter.Both
        }
        scope.processFunctionsAndConstructorsByName(info, components, constructorFilter) { symbol ->
            result = ProcessResult.FOUND
            consumeCallableCandidate(symbol, processor)
        }

        if (info.callKind == CallKind.Function && result == ProcessResult.SCOPE_EMPTY) {
            val consumedCallableValues = linkedSetOf<CfirCallableSymbol<*>>()

            fun consumeCallableValue(symbol: CfirCallableSymbol<*>) {
                if (symbol is CfirFunctionSymbol<*>) return
                if (!symbol.isDirectCallableValueCandidate()) return
                if (!consumedCallableValues.add(symbol)) return

                result = ProcessResult.FOUND
                consumeCallableCandidate(symbol, processor)
            }

            if (scope is CfirLocalScope) {
                scope.processVariablesByName(info.name, ::consumeCallableValue)
            }

            scope.processCallablesByName(info.name) { symbol ->
                consumeCallableValue(symbol)
            }
        }

        return result
    }

    private fun CfirCallableSymbol<*>.isDirectCallableValueCandidate(): Boolean {
        if (!isBound) return false
        val declaration = cfir as? CfirVariable ?: return false
        return declaration.returnTypeRef.coneTypeOrNull is ConeFunctionType
    }
}

internal class DispatchReceiverMemberScopeTowerLevel(
    private val components: BodyResolveComponents,
    private val dispatchReceiver: ReceiverValue,
    private val givenExtensionReceiver: ReceiverValue? = null,
) : CfirTowerLevel {
    private data class MemberScopeData(
        val scope: CfirScope,
        val dispatchReceiver: ReceiverValue?,
    )

    private fun memberScope(): MemberScopeData? {
        val scope = dispatchReceiver.scope(components) ?: return null
        val effectiveDispatchReceiver = dispatchReceiver.takeUnless {
            it is ExpressionReceiverValue && it.receiverExpression.resolvedQualifierSymbol(components.session) != null
        }
        return MemberScopeData(scope, effectiveDispatchReceiver)
    }

    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        val scopeData = memberScope() ?: return ProcessResult.SCOPE_EMPTY
        return ScopeBasedTowerLevel(
            components,
            scopeData.scope,
            scopeData.dispatchReceiver,
            givenExtensionReceiver,
        ).processCallablesByName(info, processor)
    }

    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        val scopeData = memberScope() ?: return ProcessResult.SCOPE_EMPTY
        return ScopeBasedTowerLevel(
            components,
            scopeData.scope,
            scopeData.dispatchReceiver,
            givenExtensionReceiver,
        ).processFunctionsByName(info, processor)
    }
}

internal class TowerLevelHandler {
    fun handleLevel(
        collector: CfirCandidateCollector,
        candidateFactory: CandidateFactory,
        info: CallInfo,
        explicitReceiverKind: ExplicitReceiverKind,
        group: CfirTowerGroup,
        towerLevel: CfirTowerLevel,
        context: ResolutionContext,
    ): ProcessResult {
        val processor = TowerLevelProcessor(
            callInfo = info,
            explicitReceiverKind = explicitReceiverKind,
            resultCollector = collector,
            candidateFactory = candidateFactory,
            group = group,
            context = context,
        )

        return when (info.callKind) {
            CallKind.NamedValueAccess -> towerLevel.processCallablesByName(info, processor)
            CallKind.Function,
            CallKind.DelegatingConstructorCall,
            CallKind.EnumConstructorCall,
            -> towerLevel.processFunctionsByName(info, processor)
        }
    }
}

internal class TowerLevelProcessor(
    val callInfo: CallInfo,
    val explicitReceiverKind: ExplicitReceiverKind,
    val resultCollector: CfirCandidateCollector,
    val candidateFactory: CandidateFactory,
    val group: CfirTowerGroup,
    val context: ResolutionContext,
) {
    fun consumeCandidate(
        symbol: CfirCallableSymbol<*>,
        scope: CfirScope?,
        dispatchReceiver: ReceiverValue? = null,
        givenExtensionReceiver: ReceiverValue? = null,
    ): CandidateApplicability {
        return resultCollector.consumeCandidate(
            group,
            candidateFactory.createCandidate(
                callInfo = callInfo,
                symbol = symbol,
                originScope = scope,
                explicitReceiverKind = explicitReceiverKind,
                dispatchReceiver = dispatchReceiver,
                givenExtensionReceiver = givenExtensionReceiver,
            ),
            context,
        )
    }

    fun consumeFunctionTypeInvokeCandidate(
        receiverExpression: org.cangnova.cangjie.cfir.expressions.CfirExpression,
        dispatchReceiver: ReceiverValue,
    ): CandidateApplicability {
        val functionType = receiverExpression.resolvedType as ConeFunctionType

        return resultCollector.consumeCandidate(
            group,
            candidateFactory.createFunctionTypeInvokeCandidate(
                callInfo = callInfo,
                functionType = functionType,
                receiverExpression = receiverExpression,
                explicitReceiverKind = explicitReceiverKind,
                dispatchReceiver = dispatchReceiver,
            ),
            context,
        )
    }
}

internal class FunctionTypeInvokeTowerLevel(
    private val receiverExpression: org.cangnova.cangjie.cfir.expressions.CfirExpression,
) : CfirTowerLevel {
    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult =
        processFunctionsByName(info, processor)

    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (info.callKind != CallKind.Function || info.name != OperatorNameConventions.INVOKE) {
            return ProcessResult.SCOPE_EMPTY
        }
        if (receiverExpression.resolvedType !is ConeFunctionType) return ProcessResult.SCOPE_EMPTY

        val functionTypeReceiver = ExpressionReceiverValue(receiverExpression)
        processor.consumeFunctionTypeInvokeCandidate(receiverExpression, functionTypeReceiver)
        return ProcessResult.FOUND
    }
}

internal fun CfirScope.mayContainName(name: org.cangnova.cangjie.name.Name): Boolean {
    if (this is CfirContainingNamesAwareScope) {
        if (name in getCallableNames() || name in getClassifierNames()) return true
    }

    var found = false
    processCallablesByName(name) { found = true }
    if (found) return true
    processFunctionsByName(name) { found = true }
    if (found) return true
    processClassifiersByName(name) { found = true }
    return found
}
