package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidate
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
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }
}
