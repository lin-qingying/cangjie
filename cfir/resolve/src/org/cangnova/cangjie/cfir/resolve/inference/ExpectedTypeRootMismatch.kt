package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidateByExpectedReturnType
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext

/**
 * 判断不可适用候选是否仅由显式目标类型消费点的根约束造成。
 *
 * 调用解析会在 completion 前把不成功候选包装成错误引用。只有候选唯一的失败来源是
 * [InapplicableCandidateByExpectedReturnType]，调用完成才可保留已解析的 callee；随后由
 * 目标类型 owner 将实际类型与目标类型比较并报告 TYPE_MISMATCH。
 */
internal fun CfirNamedReferenceWithCandidate.isExpectedTypeRootMismatchOnly(
    expression: CfirExpression,
    context: BodyResolveContext,
): Boolean {
    if (context.expectedTypeForRoot(expression) == null) return false
    if (this is CfirErrorReferenceWithCandidate &&
        diagnostic !is ConeInapplicableCandidateError &&
        diagnostic !is ConeConstraintSystemHasContradiction
    ) return false

    return candidate.diagnostics.singleOrNull() === InapplicableCandidateByExpectedReturnType &&
        candidate.errors.isEmpty() &&
        candidate.argumentMappingOutcome?.hasMappingFailure != true
}
