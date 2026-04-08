package org.cangnova.cangjie.cfir.resolve.calls.tower

import org.cangnova.cangjie.cfir.calls.ExpressionReceiverValue
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.ConstructorFilter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.processFunctionsAndConstructorsByName
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
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
    override fun processCallablesByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        var result = ProcessResult.SCOPE_EMPTY
        scope.processCallablesByName(info.name) { symbol ->
            result = ProcessResult.FOUND
            processor.consumeCandidate(symbol, scope, dispatchReceiver, givenExtensionReceiver)
        }
        return result
    }

    override fun processFunctionsByName(info: CallInfo, processor: TowerLevelProcessor): ProcessResult {
        if (info.callKind == CallKind.EnumConstructorCall) {
            var result = ProcessResult.SCOPE_EMPTY
            scope.processCallablesByName(info.name) { symbol ->
                val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return@processCallablesByName
                result = ProcessResult.FOUND
                processor.consumeCandidate(enumConstructorSymbol, scope, dispatchReceiver, givenExtensionReceiver)
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
            processor.consumeCandidate(symbol, scope, dispatchReceiver, givenExtensionReceiver)
        }

        if (info.callKind == CallKind.Function && result == ProcessResult.SCOPE_EMPTY) {
            val consumedCallableValues = linkedSetOf<CfirCallableSymbol<*>>()

            fun consumeCallableValue(symbol: CfirCallableSymbol<*>) {
                if (symbol is CfirFunctionSymbol<*>) return
                if (!symbol.isDirectCallableValueCandidate()) return
                if (!consumedCallableValues.add(symbol)) return

                result = ProcessResult.FOUND
                processor.consumeCandidate(symbol, scope, dispatchReceiver, givenExtensionReceiver)
            }

            if (scope is CfirLocalScopeImpl) {
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
        return declaration.returnTypeRef.coneTypeOrNull is ConeFuncType
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
            it is ExpressionReceiverValue && it.receiverExpression.resolvedQualifierClassifier(components.session) != null
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
        scope: CfirScope,
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
