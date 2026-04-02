package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorNamedValue
import org.cangnova.cangjie.cfir.declarations.builder.buildErrorFunction
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl

import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithSingleChild
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorNamedValueSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind

/**
 * Small construction seam for candidates discovered during tower traversal.
 *
 * This keeps tower traversal focused on scope walking while preserving the current
 * receiver/base-system defaults used by local call resolution.
 */
class CandidateFactory(
    private val context: ResolutionContext,
    private val baseSystem: ConstraintStorage  ,
) {
    constructor(context: ResolutionContext, callInfo: CallInfo) : this(context, buildBaseSystem(context, callInfo))
    companion object {
        private fun buildBaseSystemForContainingCallAwareCases(
            context: ResolutionContext,
            containingCall: Candidate,
            // For callable references, there is no call
            callInfo: CallInfo?,
        ): ConstraintStorage {
            val system = context.inferenceComponents.createConstraintSystem()
            system.setBaseSystem(containingCall.system.currentStorage())
            callInfo?.argumentAtoms?.forEach {
                system.addSubsystemFromAtom(it)
            }
            return system.asReadOnlyStorage()
        }
        private fun buildBaseSystem(context: ResolutionContext, callInfo: CallInfo): ConstraintStorage {
            callInfo.containingCandidateForCollectionLiteral?.let {
                return buildBaseSystemForContainingCallAwareCases(context, it, callInfo)
            }
            val system = context.inferenceComponents.createConstraintSystem()
            callInfo.argumentAtoms.forEach {
                system.addSubsystemFromAtom(it)
            }
            context.session.inferenceLogger?.logStage("CandidateFactory.buildBaseSystem()", system)
            return system.asReadOnlyStorage()
        }
    }
    fun createCandidate(
        callInfo: CallInfo,
        symbol: CfirCallableSymbol<*>,
        originScope: CfirScope?,
        explicitReceiverKind: ExplicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
        dispatchReceiver: ReceiverValue? = null,
        givenExtensionReceiver: ReceiverValue? = null,
    ): Candidate {
        return Candidate(
            symbol = symbol,
            dispatchReceiver = dispatchReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            givenExtensionReceiver = givenExtensionReceiver?.receiverExpression?.let(ConeResolutionAtom::createRawAtom),
            explicitReceiverKind = explicitReceiverKind,
            constraintSystemFactory = context.inferenceComponents.constraintSystemFactory,
            baseSystem = baseSystem,
            callInfo = callInfo,
            originScope = originScope,
            bodyResolveContext = context.bodyResolveContext,
        )
    }

    fun createErrorCandidate(callInfo: CallInfo, diagnostic: ConeDiagnostic): Candidate {
        val errorSymbol = when(callInfo.callKind) {
            is CallKind.Function,
                ->  createErrorFunctionSymbol(diagnostic)

            CallKind.EnumConstructorCall -> createErrorEnumConstructorSymbol(diagnostic)
            CallKind.NamedValueAccess ->
                createErrorNamedValueSymbol(callInfo, diagnostic)
        }
        val candidate = createCandidate(
            callInfo = callInfo,
            symbol = errorSymbol,
            originScope = null,
        )
        when (diagnostic) {
            is org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError ->
                candidate.addDiagnostic(HiddenCandidate())
            else ->
                candidate.addDiagnostic(InferenceConstraintError(diagnostic.reason))
        }
        return candidate
    }

    private fun createErrorFunctionSymbol(diagnostic: ConeDiagnostic): CfirErrorFunctionSymbol {
        return CfirErrorFunctionSymbol().also {
            buildErrorFunction {
                source = null
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes()
                dispatchReceiverType = null
                status = CfirDeclarationStatusImpl()
                valueParameters.clear()
                body = null
                this.diagnostic = diagnostic
                symbol = it
            }
        }
    }

    private fun createErrorEnumConstructorSymbol(diagnostic: ConeDiagnostic): CfirErrorEnumConstructorSymbol {
        val callableId = CallableId(org.cangnova.cangjie.name.Name.special("<error-enum-constructor>"))
        return CfirErrorEnumConstructorSymbol(callableId, diagnostic).also {
            buildErrorNamedValue {
                source = null
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes()
                status = CfirDeclarationStatusImpl()
                this.diagnostic = diagnostic
                name = callableId.callableName
                symbol = it
            }
        }
    }

    private fun createErrorNamedValueSymbol(
        callInfo: CallInfo,
        diagnostic: ConeDiagnostic,
    ): CfirErrorNamedValueSymbol {
        return CfirErrorNamedValueSymbol(callInfo.name.asErrorNamedValueCallableId(), diagnostic).also {
            buildErrorNamedValue {
                source = callInfo.callSite.source
                moduleData = context.session.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.Error
                attributes = CfirDeclarationAttributes()
                status = CfirDeclarationStatusImpl()
                this.diagnostic = diagnostic
                name = callInfo.name
                symbol = it
            }
        }
    }
}

private fun org.cangnova.cangjie.name.Name.asErrorNamedValueCallableId() =
    org.cangnova.cangjie.name.CallableId(this)

private fun processConstraintStorageFromAtom(
    atom: ConeResolutionAtom,
    processor: (ConstraintStorage) -> Unit,
): Boolean {
    return when (atom) {
        is ConeAtomWithCandidate -> {
            processor(atom.candidate.system.asReadOnlyStorage())
            true
        }
        is ConeResolutionAtomWithSingleChild -> {
            processConstraintStorageFromAtom(atom.subAtom ?: return false, processor)
        }
        else -> false
    }
}
fun PostponedArgumentsAnalyzerContext.addSubsystemFromAtom(atom: ConeResolutionAtom): Boolean {
    return processConstraintStorageFromAtom(atom) {
        // If a call inside a lambda uses outer CS,
        // it's already integrated into inference session via FirPCLAInferenceSession.processPartiallyResolvedCall
        if (!it.usesOuterCs) {
            addOtherSystem(it)
        }
    }
}
