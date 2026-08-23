package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.hasUncertainExpectedTypeCompatibilityShape
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains

/**
 * 在解析实参前为泛型候选注入 expected return type 约束。
 *
 * 官方泛型调用推断把 call target return 与实参约束同时求解；若返回目标晚于实参到达，
 * `id(1) + Int32(3)` 中的字面量会在返回目标到达前被默认成 Int64。因此当候选返回类型含
 * 当前候选的 fresh variable 时，确定的 expected type 必须在实参检查前进入同一约束系统。
 *
 * 返回类型已确定（proper）的候选不再在此处按目标类型剪枝：官方
 * `TypeCheckCall::CheckCandidate` 先检查实参兼容性、后比较返回类型，实参失败时直接以
 * 实参诊断终结候选且不再执行返回类型比较（`AssignExpr.cpp` 同族顺序）。该剪枝由
 * [CfirCheckExpectedReturnTypeAfterArguments] 在实参检查完成后执行。
 *
 * 函数体尾表达式不参与本阶段：声明返回类型约束属于调用完成后的类型检查，
 * 由 reduceCandidatesByExpectedReturnType 统一处理；若在实参检查前按声明返回类型淘汰，
 * 所有同名重载都会退化成 UNRESOLVED_REFERENCE，且真实实参适用性没有机会参与选择。
 */
object CfirCheckExpectedReturnTypeBeforeArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val resolutionMode = candidate.callInfo.resolutionMode
        if (resolutionMode is ResolutionMode.WithExpectedType && resolutionMode.lastStatementInBlock) {
            // 见类 KDoc：尾表达式的返回类型细化由候选完成后的 reduceCandidatesByExpectedReturnType
            // 负责，并在没有匹配返回类型时保留原候选集合。
            return
        }
        val expectedType = resolutionMode.expectedType ?: return

        // 隐式返回类型必须通过共享 returnTypeCalculator 读取；声明 returnTypeRef 在该阶段
        // 仍可能不是 resolved ref，直接调用 candidate.substitutedReturnType 会退化成错误类型。
        val candidateReturnType = context.bodyResolveComponents
            .initialTypeOfCandidate(candidate)
            .fullyExpandedType(context.session)
        /*
         * 无参泛型 enum constructor 是值构造语法，不是通过返回值协变关系反推泛型实参的普通函数。
         * 只有同一个 owner 的目标 enum 类型才能将其定型；该规则由 CfirCallCompleter
         * 的 enum completion 统一执行。这里若提前用任意 expected type 过滤/约束，
         * `Option<T>.None` 赋给其它类型时会丢失裸泛型或赋值不匹配的真实诊断。
         */
        if (candidate.isImplicitNoArgumentEnumConstructor() &&
            !candidate.system.isProperType(candidateReturnType)
        ) return
        if (candidate.system.isProperType(candidateReturnType)) {
            // 具体返回类型的 expected-return 剪枝已移至实参检查之后，见类 KDoc。
            return
        }

        /*
         * 官方泛型调用推断把 call target return 与实参约束同时求解。这里仅为当前
         * candidate 的 fresh return variable 注入确定的 expected type；外层 PCLA
         * placeholder、错误恢复类型和不确定 expected type 仍保持延迟，不提前淘汰候选。
         */
        if (
            candidate.argumentMappingOutcome?.hasMappingFailure != true &&
            candidateReturnType.containsCurrentCandidateInferenceVariable(candidate) &&
            candidate.system.isProperType(expectedType) &&
            !expectedType.hasUncertainExpectedTypeCompatibilityShape()
        ) {
            candidate.system.addSubtypeConstraint(
                candidateReturnType,
                expectedType,
                ConeExpectedTypeConstraintPosition,
            )
        }
    }

    /** 判断返回类型是否真正引用当前候选尚未固定的 fresh variable。 */
    private fun ConeCangJieType.containsCurrentCandidateInferenceVariable(candidate: Candidate): Boolean {
        val notFixedVariables = candidate.system.currentStorage().notFixedTypeVariables
        return contains { type ->
            type is ConeTypeVariableType && type.typeConstructor in notFixedVariables
        }
    }

    /** 只有未显式实例化的无参 enum constructor 才可能需要 owner target typing。 */
    private fun Candidate.isImplicitNoArgumentEnumConstructor(): Boolean =
        !callInfo.hasExplicitTypeArguments &&
        (symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor)
            ?.valueParameters
            ?.isEmpty() == true
}
