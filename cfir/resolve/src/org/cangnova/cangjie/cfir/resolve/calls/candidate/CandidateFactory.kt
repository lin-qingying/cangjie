package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.calls.ReceiverValue
import org.cangnova.cangjie.cfir.diagnostic.HiddenCandidate
import org.cangnova.cangjie.cfir.diagnostic.InferenceConstraintError
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl

import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithSingleChild
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
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
        val errorSymbol = createErrorFunctionSymbol(diagnostic)
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

    private fun createErrorFunctionSymbol(diagnostic: ConeDiagnostic): CfirNamedFunctionSymbol {
        val symbol = CfirNamedFunctionSymbol(CallableId(SpecialNames.NO_NAME_PROVIDED))
        val declaration = buildNamedFunction {
            source = null
            moduleData = context.session.moduleData
            origin = CfirDeclarationOrigin.Synthetic.FakeFunction
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl.DEFAULT
            returnTypeRef = buildErrorTypeRef { this.diagnostic = diagnostic }
            this.symbol = symbol
            name = Name.identifier("<error>")
            isMut = false
        }
        symbol.bind(declaration)
        return symbol
    }
}

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
