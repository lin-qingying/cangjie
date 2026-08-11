package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidateByExpectedReturnType
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.hasUncertainExpectedTypeCompatibilityShape
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.safeSubstitute

/**
 * 在解析实参前按 expected return type 保守淘汰确定不兼容的候选。
 *
 * fresh substitutor 建立后，非泛型返回类型已经能参与普通 subtype 判断；先淘汰这些确定
 * 不可能满足目标类型的 overload，可避免为每个错误返回类型递归解析完整的嵌套实参调用。
 * 含未固定变量或错误恢复类型的候选仍交给后续实参检查和最终 expected-return 规约。
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
        if (!candidate.system.isProperType(candidateReturnType)) return
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
}
