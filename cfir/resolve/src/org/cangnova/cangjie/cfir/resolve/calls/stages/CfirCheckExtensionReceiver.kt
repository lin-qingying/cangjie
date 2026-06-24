package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides

/**
 * 检查仓颉 extend receiver。
 *
 * Kotlin FIR 在 callable 上直接保存 receiver parameter；仓颉 CFIR 把 receiver 类型保存在
 * owner extend 的 `extendedTypeRef` 中。因此本阶段以 owner extend 为接收者类型来源，
 * 再复用 providers 层的 extend target 匹配规则得到当前 use-site 的具体 receiver 类型。
 */
object CfirCheckExtensionReceiver : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 检查候选的给定 extension receiver 是否可转换为 owner extend 要求的接收者类型。 */
    override suspend fun check(candidate: Candidate) {
        val receiver = candidate.givenExtensionReceiver ?: return
        val expectedReceiverType = candidate.expectedExtensionReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType)
        val actualType = receiver.expression.coneTypeOrNull ?: return

        ArgumentCheckingProcessor.resolvePlainArgumentType(
            candidate = candidate,
            atom = receiver,
            argumentType = actualType,
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = true,
            isDispatch = false,
            sourceForReceiver = candidate.callInfo.callSite.source,
        )

        candidate.chosenExtensionReceiver = receiver
        sink.yieldIfNeed()
    }

    context(context: ResolutionContext)
    /** 计算候选在当前 use-site 下实际期望的 extend receiver 类型。 */
    private fun Candidate.expectedExtensionReceiverType(): ConeCangJieType? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        val originalSymbol = callableSymbol.unwrapSubstitutionOverrides()
        val extendProvider = context.session.extendProvider
        val ownerExtend = extendProvider.getContainingExtend(originalSymbol)
            ?.takeIf(extendProvider::isExtendAccessible)
            ?: return null
        val actualReceiverType = givenExtensionReceiver?.expression?.coneTypeOrNull
        if (actualReceiverType != null) {
            findExtendDeclarationSubstitution(context.session, ownerExtend, actualReceiverType)
                ?.substitutedReceiverType
                ?.let { return it }
        }
        return ownerExtend.extendedTypeRef.coneTypeOrNull
    }
}
