package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl

data class ReturnArgumentsAnalysisResult(
    val returnArguments: Collection<ConeResolutionAtom>,
    val additionalConstraints: ConstraintStorage?,
)

/**
 * Lambda 分析器接口。
 *
 * 负责对已解析的 lambda atom 执行完整的语义分析，
 * 包括参数类型写入、返回类型处理和 PCLA 会话管理。
 *
 * 对齐 K2 `LambdaAnalyzer`。
 */
interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaAtom: ConeResolvedLambdaAtom,
        parameters: List<ConeCangJieType>,
        expectedReturnType: ConeCangJieType?,
        candidate: Candidate,
        withPCLASession: Boolean,
        forOverloadByLambdaReturnType: Boolean,
    ): ReturnArgumentsAnalysisResult
}

/**
 * 延迟参数分析器。
 *
 * 按 atom 类型分发到对应的分析逻辑：
 * - [ConeResolvedLambdaAtom] → 通过 [LambdaAnalyzer] 分析 lambda 体
 * - 其他延迟 atom 类型 → 待后续实现
 *
 * 对齐 K2 `PostponedArgumentsAnalyzer`。
 */
class PostponedArgumentsAnalyzer(
    private val resolutionContext: ResolutionContext,
    private val lambdaAnalyzer: LambdaAnalyzer,
    private val components: InferenceComponents,
    private val callResolver: CfirCallResolver,
) {
    /**
     * 分析单个延迟 atom。
     *
     * @param csImpl 约束系统实现
     * @param atom 待分析的延迟 atom
     * @param candidate 所属候选
     * @param withPCLASession 是否在 PCLA 会话中分析
     */
    fun analyze(
        csImpl: ConstraintSystemImpl,
        atom: ConePostponedResolvedAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        when (atom) {
            is ConeResolvedLambdaAtom -> {
                analyzeLambda(csImpl, atom, candidate, withPCLASession)
            }
            else -> {
                // 其他延迟 atom 类型的分析逻辑待后续实现
                atom.analyzed = true
            }
        }
    }

    private fun analyzeLambda(
        csImpl: ConstraintSystemImpl,
        atom: ConeResolvedLambdaAtom,
        candidate: Candidate,
        withPCLASession: Boolean,
    ) {
        val currentSubstitutor = csImpl.buildCurrentSubstitutor()
        val parameterTypes = atom.parameterTypes.map {
            currentSubstitutor.safeSubstitute(csImpl, it) as ConeCangJieType
        }
        val expectedReturnType = atom.returnType.let {
            currentSubstitutor.safeSubstitute(csImpl, it) as ConeCangJieType
        }

        val result = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            atom, parameterTypes, expectedReturnType, candidate,
            withPCLASession, forOverloadByLambdaReturnType = false,
        )

        atom.analyzed = true
        atom.returnStatements = result.returnArguments

        if (result.additionalConstraints != null) {
            csImpl.addOtherSystem(result.additionalConstraints)
        }
    }
}

fun ConeLambdaWithTypeVariableAsExpectedTypeAtom.transformToResolvedLambda(
    csBuilder: ConstraintSystemBuilder,
    context: ResolutionContext,
    expectedType: ConeCangJieType? = null,
    returnTypeVariable: ConeTypeVariableForLambdaReturnType? = null,
): ConeResolvedLambdaAtom {
    val fixedExpectedType = csBuilder.buildCurrentSubstitutor().asCone()
        .substituteOrSelf(expectedType ?: this.expectedType)
    val resolvedAtom = ArgumentCheckingProcessor.createResolvedLambdaAtomDuringCompletion(
        candidateOfOuterCall, csBuilder, ConeResolutionAtomWithPostponedChild(expression), fixedExpectedType,
        context, returnTypeVariable = returnTypeVariable,
        anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
    )

    subAtom = resolvedAtom
    analyzed = true

    return resolvedAtom
}