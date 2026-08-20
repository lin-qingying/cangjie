package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidateByExpectedReturnType
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.hasUncertainExpectedTypeCompatibilityShape
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.safeSubstitute

/**
 * 在解析实参前按 expected return type 保守淘汰确定不兼容的候选。
 *
 * fresh substitutor 建立后，非泛型返回类型已经能参与普通 subtype 判断；先淘汰这些确定
 * 不可能满足目标类型的 overload，可避免为每个错误返回类型递归解析完整的嵌套实参调用。
 * 当返回类型含当前候选的 fresh variable 时，目标类型必须在实参检查前进入同一约束系统；
 * 否则 `id(1) + Int32(3)` 中的字面量会在返回目标到达前被默认成 Int64。
 */
object CfirCheckExpectedReturnTypeBeforeArguments : ResolutionStage() {
    /**
     * 仅当候选返回类型与目标类型都已确定时执行早期适用性检查。
     */
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val resolutionMode = candidate.callInfo.resolutionMode
        if (resolutionMode is ResolutionMode.WithExpectedType && resolutionMode.lastStatementInBlock) {
            // 函数体尾表达式的声明返回类型约束属于调用完成后的类型检查：例如
            // `main(): Int64 { print(value) }` 必须先把合法的 `print(...): Unit` 调用解析完成，
            // 再由声明体报告 Unit 与 Int64 不匹配。若在实参检查前按 Int64 淘汰 print，
            // 所有同名重载都会退化成 UNRESOLVED_REFERENCE，且真实实参适用性没有机会参与选择。
            // 非尾表达式仍保留本阶段的早期剪枝；尾表达式的重载返回类型细化由候选完成后的
            // reduceCandidatesByExpectedReturnType 负责，并在没有匹配返回类型时保留原候选集合。
            return
        }
        val expectedType = candidate.callInfo.resolutionMode.expectedType ?: return

        val currentSubstitutor = candidate.system.buildCurrentSubstitutor()

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
         *
         * 仅跳过仍含 fresh variable 的隐式调用。非泛型构造器、已由 receiver 定型的构造器
         * 以及显式类型实参调用都已有确定结果类型，仍应走普通 expected-return 过滤，以便
         * 保留外层 `TYPE_MISMATCH`。
         */
        if (candidate.isImplicitNoArgumentEnumConstructor() &&
            !candidate.system.isProperType(candidateReturnType)
        ) return
        if (!candidate.system.isProperType(candidateReturnType)) {
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
            return
        }
        if (candidateReturnType.hasUncertainExpectedTypeCompatibilityShape()) return

        val currentExpectedType = currentSubstitutor
            .safeSubstitute(candidate.system, expectedType)
            .asCone()
            .fullyExpandedType(context.session)
        if (!candidate.system.isProperType(currentExpectedType)) return
        if (currentExpectedType.hasUncertainExpectedTypeCompatibilityShape()) return

        if (!AbstractTypeChecker.isSubtypeOf(context.session.typeContext, candidateReturnType, currentExpectedType)) {
            sink.yieldDiagnostic(InapplicableCandidateByExpectedReturnType)
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
