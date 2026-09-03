package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidateByExpectedReturnType
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.isOperatorOperandInference
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.hasUncertainExpectedTypeCompatibilityShape
import org.cangnova.cangjie.cfir.resolve.calls.isBareNoArgumentEnumValueAccess
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.safeSubstitute

/**
 * 在实参检查完成后按 expected return type 淘汰确定不兼容的候选。
 *
 * 官方 `TypeCheckCall::CheckCandidate` 的顺序是先实参兼容性（:1382-1388），后返回类型
 * 与目标比较（:1395-1406）；实参失败时立即以实参诊断终结候选，返回类型比较不再执行，
 * 重放的诊断因此只含实参级错误。本阶段对齐该顺序：
 *
 * - 候选在实参检查阶段已记录任何失败（映射失败/解析诊断/约束矛盾）时直接跳过，
 *   不再叠加返回类型诊断；
 * - 仅当返回类型为具体类型（无 fresh variable）且目标类型同样确定时才比较，
 *   泛型返回类型的期望约束注入仍由 [CfirCheckExpectedReturnTypeBeforeArguments] 在
 *   实参检查前完成。
 *
 * 函数体尾表达式的返回类型细化由候选完成后的 reduceCandidatesByExpectedReturnType
 * 统一处理，本阶段不参与。
 */
object CfirCheckExpectedReturnTypeAfterArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val resolutionMode = candidate.callInfo.resolutionMode
        if (resolutionMode is ResolutionMode.WithExpectedType && resolutionMode.lastStatementInBlock) {
            return
        }
        val expectedType = resolutionMode.expectedType ?: return
        if (resolutionMode.isOperatorOperandInference) return

        // 裸无参 enum case 是值访问，不是以返回类型参与 overload applicability 的函数调用。
        // 其表达式类型应保留为 enum owner，外层参数/操作符检查再报告真实的不兼容诊断；
        // 显式 `Entry()` 的 callSite 是 CfirFunctionCall，不会走此分支。
        if (candidate.isBareNoArgumentEnumValueAccess()) return

        // 对齐官方 CheckCandidate：实参兼容性失败时提前返回，返回类型比较不执行。
        if (candidate.argumentMappingOutcome?.hasMappingFailure == true) return
        if (candidate.diagnostics.isNotEmpty() || candidate.errors.isNotEmpty()) return

        val candidateReturnType = context.bodyResolveComponents
            .initialTypeOfCandidate(candidate)
            .fullyExpandedType(context.session)
        if (!candidate.system.isProperType(candidateReturnType)) return
        if (candidateReturnType.hasUncertainExpectedTypeCompatibilityShape()) return

        val currentSubstitutor = candidate.system.buildCurrentSubstitutor()
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
