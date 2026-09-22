package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.IllegalAccessNonStaticMember
import org.cangnova.cangjie.cfir.diagnostic.ObjectCannotAccessStaticMember
import org.cangnova.cangjie.cfir.resolve.calls.CfirMemberAccessFormError
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldIfNeed
import org.cangnova.cangjie.cfir.resolve.calls.isTypeQualifierReceiver
import org.cangnova.cangjie.cfir.resolve.calls.memberAccessFormError
import org.cangnova.cangjie.cfir.resolve.calls.noArgEnumConstructorTargetType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type

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
        // 非 callable 候选不参与形态判定；后续 receiver 类型约束同样只对 callable 成立。
        val callableSymbol = candidate.symbol as? CfirCallableSymbol<*> ?: return
        val memberAccessFormError = memberAccessFormError(
            callableSymbol,
            candidate.callInfo,
            candidate.dispatchReceiver?.expression,
            context,
        )
        when (memberAccessFormError) {
            is CfirMemberAccessFormError.ObjectAccessedStaticMember -> {
                sink.reportDiagnostic(ObjectCannotAccessStaticMember(memberAccessFormError.memberName))
                sink.yieldIfNeed()
                return
            }

            is CfirMemberAccessFormError.TypeQualifierAccessedNonStaticMember -> {
                sink.reportDiagnostic(IllegalAccessNonStaticMember(memberAccessFormError.memberName))
                sink.yieldIfNeed()
                return
            }

            null -> Unit
        }
        if (candidate.isImplicitReceiverForStaticMember()) return

        val expectedReceiverType = candidate.expectedDispatchReceiverType() ?: return
        val expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType)
        val targetTypedEnumReceiverType = receiver.expression.noArgEnumConstructorTargetType(expectedType, context.session)
            ?.also { receiver.expression.replaceConeTypeOrNull(it) }
        val actualType = targetTypedEnumReceiverType ?: receiver.expression.coneTypeOrNull ?: return
        if (candidate.isStaticQualifierDispatchReceiver(context)) return

        if (targetTypedEnumReceiverType != null) {
            candidate.system.addSubtypeConstraint(
                actualType,
                expectedType,
                ConeReceiverConstraintPosition(receiver.expression, candidate.callInfo.callSite.source),
            )
            sink.yieldIfNeed()
            return
        }

        /*
         * 成员声明的 dispatch receiver 可能含当前候选的 owner fresh type variable，
         * 例如 `Option<T>.getOrThrow()`。tower 已经选中了 nominal receiver 候选，
         * 但仍必须把 `Option<IdealInt> <: Option<T>` 这类同构 receiver 约束送进
         * 参数检查共享层，让 owner `T` 参与 completion。
         */
        if (candidate.expectedReceiverContainsCurrentInferenceVariable(expectedType)) {
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
            return
        }

        /*
         * 无上下文 lambda 参数的 receiver 是官方 `SynLamExpr` placeholder。
         * 对这种 receiver，成员候选本身就是推断输入，不能先用普通 receiver
         * 适用性检查把候选判成 `InapplicableWrongReceiver`；后续实参检查和
         * completion 会继续筛选候选并固定该 type variable。
         *
         * 成员候选为泛型 owner 创建的占位实参变量（官方 `ConstrainByCtor` 的 T-Fly）
         * 属于 lambda 自身的求解系统：声明侧接收者类型含这类变量时（如
         * `Array<TypeVariable(T)>`），不能把 `x <: Array<T-Fly>` 写进正在完成的系统，
         * 否则形参在 body 末求解前就被定型成含未解变量的非 proper 类型。此时跳过
         * 约束写入，保持形参开放，交由语句边界/body 末/调用点证据定型。
         */
        if (actualType.isFreshLambdaReceiverTypeVariable()) {
            val expectedContainsSessionVariable = context.bodyResolveContext.inferenceSession
                .containsRegisteredInferenceVariable(expectedType)
            if (!expectedContainsSessionVariable) {
                candidate.system.addSubtypeConstraint(
                    actualType,
                    expectedType,
                    ConeReceiverConstraintPosition(receiver.expression, candidate.callInfo.callSite.source),
                )
            }
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

    /** 判断 receiver 期望类型中是否含当前候选约束系统的 fresh type variable。 */
    private fun Candidate.expectedReceiverContainsCurrentInferenceVariable(type: ConeCangJieType): Boolean {
        val currentVariables = system.currentStorage().allTypeVariables
        fun ConeCangJieType.containsCurrentVariable(): Boolean = when (this) {
            is ConeTypeVariableType -> typeConstructor in currentVariables
            is ConeLookupTagBasedType -> typeArguments.any { it.type.containsCurrentVariable() }
            is ConeFunctionType -> parameterTypes.any { it.containsCurrentVariable() } ||
                    returnType.containsCurrentVariable()
            is ConeTupleType -> elementTypes.any { it.containsCurrentVariable() }
            is ConeVArrayType -> elementType.containsCurrentVariable()
            else -> false
        }
        return type.containsCurrentVariable()
    }

    /*
     * 「接收者形态 × 成员 static 属性」的一致性判定由 [memberAccessFormError] 统一持有：
     * 官方在成员发现阶段就按 `Attribute::STATIC` 过滤目标集，该判定先于访问控制成立，
     * 因此候选检查阶段与发现/诊断阶段必须读取同一份规则，不能各自维护一份分支。
     */

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

    /**
     * 无显式接收者访问 static 成员时，隐式 receiver 只负责把成员 scope 暴露给 tower。
     *
     * owner 泛型替换已经在候选 fresh-variable 初始化阶段从 use-site receiver 建立；
     * 这里若继续加入 dispatch receiver subtype 约束，会把 extend/interface 继承路径上的
     * 查找 receiver 当作运行时实例 receiver，导致不同声明来源的同名类型参数误报不匹配。
     */
    private fun Candidate.isImplicitReceiverForStaticMember(): Boolean {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        if (!callableSymbol.cfir.status.isStatic) return false
        if (callInfo.explicitReceiver != null) return false
        return dispatchReceiver != null
    }

    /** 当前 dispatch receiver 是否为 class/typealias/内建类型 qualifier。 */
    private fun Candidate.hasTypeQualifierDispatchReceiver(context: ResolutionContext): Boolean =
        dispatchReceiver?.expression.isTypeQualifierReceiver(context)

    /** 判断类型是否为无上下文 lambda receiver 使用的 fresh type variable。 */
    private fun ConeCangJieType.isFreshLambdaReceiverTypeVariable(): Boolean =
        this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null
}
