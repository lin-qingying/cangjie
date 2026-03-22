package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.constraints.CfirConstraint
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 仓颉新架构下的调用补全入口。
 *
 * 约束求解在 CfirInferTypeArguments 阶段已完成（fixAllVariables + buildResult），
 * 此类的职责是：
 * 1. 补充期望类型约束（若有）并重新求解
 * 2. 应用 substitutor 到签名返回类型
 */
class CfirCallCompleter(
    private val transformer: CfirAbstractBodyResolveTransformerDispatcher,
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
) {
    val session: CfirSession = components.session

    /**
     * 完成候选的约束系统并返回代入后的返回类型。
     */
    fun completedResultType(
        candidate: CfirCandidate,
        resolutionMode: CfirResolutionMode = CfirResolutionMode.ContextIndependent,
    ): ConeCangJieType? {
        addConstraintFromExpectedTypeIfNeeded(candidate, resolutionMode)
        return candidate.substitutedReturnType()
    }

    private fun addConstraintFromExpectedTypeIfNeeded(
        candidate: CfirCandidate,
        resolutionMode: CfirResolutionMode,
    ) {
        val expectedType = (resolutionMode as? CfirResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType ?: return
        val signatureType = candidate.signatureReturnType() ?: return
        val constraintSystem = candidate.constraintSystem ?: return
        session.inferenceLogger?.apply {
            logCandidate(candidate)
            logStage("CompleteCallFromExpectedType", constraintSystem)
        }

        constraintSystem.addConstraint(
            CfirConstraint.ExpectedType(actual = signatureType, expected = expectedType)
        )
        // 重新求解（fixAllVariables 是幂等的，已固定变量不变，新约束可能影响未固定变量）
        constraintSystem.fixAllVariables()
        candidate.updateSubstitutor(constraintSystem.buildResult().substitutor)
    }
}
