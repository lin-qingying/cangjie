package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
import org.cangnova.cangjie.cfir.diagnostic.IllegalAccessNonStaticMember
import org.cangnova.cangjie.cfir.diagnostic.ObjectCannotAccessStaticMember
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

/**
 * 检查 dispatch receiver 与成员声明接收者类型的约束。
 *
 * Kotlin FIR 在 `CreateFreshTypeVariableSubstitutorStage` 后执行 `CheckDispatchReceiver`。
 * 普通成员候选的 receiver 适配已经由 tower/member scope 决定；本阶段只保留
 * dispatch receiver 自身才能判断的诊断，以及 fresh lambda receiver 的推断输入。
 */
object CfirCheckDispatchReceiver : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 检查 dispatch receiver 的实际类型是否满足成员声明要求的接收者类型。 */
    override suspend fun check(candidate: Candidate) {
        val receiver = candidate.dispatchReceiver ?: return
        candidate.objectAccessedStaticMember(context)?.let { memberName ->
            sink.reportDiagnostic(ObjectCannotAccessStaticMember(memberName))
            sink.yieldIfNeed()
            return
        }
        candidate.typeQualifierAccessedNonStaticMember(context)?.let { memberName ->
            sink.reportDiagnostic(IllegalAccessNonStaticMember(memberName))
            sink.yieldIfNeed()
            return
        }

        val expectedReceiverType = candidate.expectedDispatchReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType)
        val actualType = receiver.expression.coneTypeOrNull ?: return
        if (candidate.isStaticQualifierDispatchReceiver(context)) return

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

        sink.yieldIfNeed()
    }

    /** 读取候选 callable symbol 声明的 dispatch receiver 类型。 */
    private fun Candidate.expectedDispatchReceiverType(): ConeCangJieType? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        return callableSymbol.dispatchReceiverType
    }

    /**
     * 对象接收者不能访问 static 成员。
     *
     * static 成员通过 class/typealias qualifier 访问时，receiver 只是查找限定符；
     * 通过真实表达式接收者访问时，官方会过滤该 static 候选并报告专用诊断。
     */
    private fun Candidate.objectAccessedStaticMember(context: ResolutionContext): Name? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        if (!callableSymbol.cfir.status.isStatic) return null
        if (callInfo.explicitReceiver == null) return null
        if (dispatchReceiver == null) return null
        if (isStaticQualifierDispatchReceiver(context)) return null
        return callableSymbol.name
    }

    /**
     * 类型名不能访问实例成员。
     *
     * static qualifier scope 会保留实例成员候选，让这一阶段产出官方专用诊断；
     * 否则候选在 scope 层被过滤后只能退化成 `NOT_MEMBER_OF`。
     */
    private fun Candidate.typeQualifierAccessedNonStaticMember(context: ResolutionContext): Name? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        if (callableSymbol is CfirEnumConstructorSymbol) return null
        if (callableSymbol.cfir.status.isStatic) return null
        if (callInfo.explicitReceiver == null) return null
        if (!callInfo.isMemberSyntaxOrSubscriptAccess()) return null
        if (!hasTypeQualifierDispatchReceiver(context)) return null
        return callableSymbol.name
    }

    /**
     * 官方只在成员访问语法和下标 get/set 上报告“类型名访问实例成员”。
     *
     * 普通二元/一元操作符里的裸类型名仍按表达式位置处理为 `REF_NOT_BE_TYPE`，
     * 不能因为候选里有同名实例 operator 就改报非静态成员访问。
     */
    private fun org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo.isMemberSyntaxOrSubscriptAccess(): Boolean {
        if (origin != CfirFunctionCallOrigin.Operator) return true
        return name == OperatorNameConventions.GET || name == OperatorNameConventions.SET
    }

    /**
     * static 成员的类型 qualifier 是名字查找 base expression，不是运行时值接收者。
     *
     * 官方调用检查先用 base expression 建立 owner 泛型映射，再让参数/返回值约束继续推断；
     * 如果在这里把 class/typealias/内建类型 qualifier 当普通 dispatch receiver 做 subtype
     * 适用性检查，`C<T>.f`、`CString.f` 和 `I2<K> <: I1<..., K>` 这类 static 调用会被过早
     * 判为 receiver 不适用，并把后续实参级约束级联成错误诊断。
     */
    private fun Candidate.isStaticQualifierDispatchReceiver(context: ResolutionContext): Boolean {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        if (!callableSymbol.cfir.status.isStatic) return false
        return hasTypeQualifierDispatchReceiver(context)
    }

    /** 当前 dispatch receiver 是否为 class/typealias/内建类型 qualifier。 */
    private fun Candidate.hasTypeQualifierDispatchReceiver(context: ResolutionContext): Boolean {
        val receiverExpression = dispatchReceiver?.expression ?: return false
        return receiverExpression.qualifierScopeOrNull(
            context.session,
            context.bodyResolveComponents.scopeSession,
        ) != null
    }

    /** 判断类型是否为无上下文 lambda receiver 使用的 fresh type variable。 */
    private fun ConeCangJieType.isFreshLambdaReceiverTypeVariable(): Boolean =
        this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null
}
