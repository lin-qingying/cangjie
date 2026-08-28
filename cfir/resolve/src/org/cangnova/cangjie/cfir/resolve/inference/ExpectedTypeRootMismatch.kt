package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidateByExpectedReturnType
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch

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

    val expectedReturnFilterMismatch = candidate.diagnostics.singleOrNull() === InapplicableCandidateByExpectedReturnType &&
        candidate.errors.isEmpty()
    return (expectedReturnFilterMismatch || candidate.hasExplicitTypeArgumentExpectedTypeMismatch()) &&
        candidate.argumentMappingOutcome?.hasMappingFailure != true
}

/**
 * 显式类型实参已经唯一确定调用的名义结果类型；若唯一的矛盾来自根 expected type，
 * 这是赋值/初始化不匹配而非泛型实参推断失败。普通隐式泛型调用仍由推断诊断负责，
 * 因而不能把它们的同类约束冲突归为根类型不匹配。
 */
private fun Candidate.hasExplicitTypeArgumentExpectedTypeMismatch(): Boolean =
    callInfo.hasExplicitTypeArguments &&
        errors.any { error ->
            error is ConstraintMismatch && error.position.from is ConeExpectedTypeConstraintPosition
        } &&
        (
            diagnostics.isEmpty() ||
                diagnostics.any { diagnostic ->
                    diagnostic is ArgumentTypeMismatch || diagnostic == ErrorTypeInArguments
                }
            )
