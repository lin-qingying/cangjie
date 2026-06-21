package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.calls.resolvedQualifierSymbol
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.session.CfirSession
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
        if (candidate.isStaticQualifierDispatchReceiver(context.session)) return

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

    /**
     * static 成员的 class/typealias qualifier 是名字查找 base expression，不是运行时值接收者。
     *
     * 官方调用检查先用 base expression 建立 owner 泛型映射，再让参数/返回值约束继续推断；
     * 如果在这里把 qualifier 当普通 dispatch receiver 做 subtype 适用性检查，`C<T>.f`
     * 和 `I2<K> <: I1<..., K>` 这类仍含待推断 owner 参数的 static 调用会被过早判为
     * receiver 不适用，并把后续实参级约束级联成 `ARGUMENT_TYPE_MISMATCH`。
     */
    private fun Candidate.isStaticQualifierDispatchReceiver(session: CfirSession): Boolean {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        if (!callableSymbol.cfir.status.isStatic) return false
        val receiverExpression = dispatchReceiver?.expression ?: return false
        return receiverExpression.resolvedQualifierSymbol(session) != null
    }

    private fun ConeCangJieType.isFreshLambdaReceiverTypeVariable(): Boolean =
        this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null
}
