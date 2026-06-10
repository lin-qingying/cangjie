package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidate
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.getExpectedType
import org.cangnova.cangjie.cfir.resolve.transformers.ensureResolvedTypeDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import kotlin.compareTo
import kotlin.text.get

object CfirCheckArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        if (!candidate.argumentMappingInitialized) return

        val contextArgumentsOfInvoke = candidate.expectedContextParameterCountForInvoke ?: 0
        val argumentMapping = candidate.argumentMapping

        for ((index, argument) in candidate.arguments.withIndex()) {
            if (index < contextArgumentsOfInvoke) continue

            val expression = argument.expression
//            if (expression.isInaccessibleAndInapplicable()) {
//                sink.reportDiagnostic(expression.toInaccessibleReceiverDiagnostic())
//            }

            val parameter = argumentMapping[argument]
            candidate.resolveArgument(
                candidate.callInfo,
                argument,
                parameter,
                isReceiver = index == 0
            )
        }

        when {
            candidate.system.hasContradiction && candidate.callInfo.arguments.isNotEmpty() -> {
                sink.yieldDiagnostic(InapplicableCandidate)
            }
        }
    }

    context(sink: CheckerSink, context: ResolutionContext)
    private fun Candidate.resolveArgument(
        callInfo: CallInfo,
        atom: ConeResolutionAtom,
        parameter: CfirValueParameter?,
        isReceiver: Boolean,
    ) {
        // Lambdas and callable references can be unresolved at this point
        val argument = atom.expression
        argument.coneTypeOrNull.ensureResolvedTypeDeclaration(context.session)
        val expectedType =
            prepareExpectedType(context.session, callInfo, argument, parameter)
        ArgumentCheckingProcessor.resolveArgumentExpression(
            this,
            atom,
            expectedType,
            sink,
            context,
            isReceiver,
            false
        )
    }
}

private fun getExpectedTypeWithImplicitIntegerCoercion(
    session: CfirSession,
    argument: CfirExpression,
    parameter: CfirValueParameter,
    candidateExpectedType: ConeCangJieType
): ConeCangJieType? {
    return null
//    if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion)) return null

}
context(context: ResolutionContext)
private fun Candidate.prepareExpectedType(
    session: CfirSession,
    callInfo: CallInfo,
    argument: CfirExpression,
    parameter: CfirValueParameter?,
): ConeCangJieType? {
    if (parameter == null) return null
    val basicExpectedType = argument.getExpectedType(session, parameter)

    // 仓颉没有 SAM 转换，直接跳过那一步
    val expectedType =
        getExpectedTypeWithImplicitIntegerCoercion(session, argument, parameter, basicExpectedType)
            ?: basicExpectedType

    return this.substitutor.substituteOrSelf(expectedType)
}
