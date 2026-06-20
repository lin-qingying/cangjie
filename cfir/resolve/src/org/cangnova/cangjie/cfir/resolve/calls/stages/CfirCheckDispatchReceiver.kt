package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 检查 dispatch receiver 与成员声明接收者类型的约束。
 *
 * Kotlin FIR 在 `CreateFreshTypeVariableSubstitutorStage` 后执行 `CheckDispatchReceiver`。
 * CFIR 也必须在同一阶段把显式/隐式 dispatch receiver 写入候选约束系统，
 * 否则 lambda 参数这类 fresh type variable 无法从成员调用反推出接收者类型。
 */
object CfirCheckDispatchReceiver : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val receiver = candidate.dispatchReceiver ?: return
        val expectedReceiverType = candidate.expectedDispatchReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType)
        val actualType = receiver.expression.coneTypeOrNull ?: return

        /*
         * 无上下文 lambda 参数的 receiver 是官方 `SynLamExpr` placeholder。
         * 对这种 receiver，成员候选本身就是推断输入，不能先用普通 receiver
         * 适用性检查把候选判成 `InapplicableWrongReceiver`；后续实参检查和
         * completion 会继续筛选候选并固定该 type variable。
         */
        if (actualType.isFreshLambdaReceiverTypeVariable()) {
            candidate.system.addSubtypeConstraint(
                actualType,
                expectedType,
                ConeReceiverConstraintPosition(receiver.expression, candidate.callInfo.callSite.source),
            )
            sink.yieldIfNeed()
            return
        }

        ArgumentCheckingProcessor.resolvePlainArgumentType(
            candidate = candidate,
            atom = receiver,
            argumentType = actualType,
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = true,
            isDispatch = true,
            sourceForReceiver = candidate.callInfo.callSite.source,
        )
        sink.yieldIfNeed()
    }

    private fun Candidate.expectedDispatchReceiverType(): ConeCangJieType? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        return callableSymbol.dispatchReceiverType
    }

    private fun ConeCangJieType.isFreshLambdaReceiverTypeVariable(): Boolean =
        this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null
}
