package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 无上下文 lambda 在定义点重查 body 所需的树快照。
 *
 * 对应官方 ResetLambdaForReinfer：先从当前约束解构造签名，再恢复 body 并用该签名重查。
 * 此对象仅存活于 synthetic accept 的完成过程，不发布到变量，也不接收后续调用的实参。
 */
internal class CfirLambdaBodyReinferenceData(
    val lambdaExpression: CfirAnonymousFunctionExpression,
    private val preBodyResolveSnapshot: CfirResolutionSnapshot,
) {
    /** 在恢复树之前计算签名，确保参数 placeholder 仍可由当前 substitutor 替换。 */
    fun applyCompletionResult(
        substitutor: ConeSubstitutor,
        restoreBodyResolveState: Boolean = false,
    ): Boolean {
        val lambda = lambdaExpression.anonymousFunction
        val previousType = lambda.typeRef.coneTypeOrNull as? ConeFunctionType ?: return false
        val parameterTypes = lambda.valueParameters.map { parameter ->
            parameter.returnTypeRef.coneTypeOrNull?.substituteForDeclaration(substitutor) ?: return false
        }
        val returnType = lambda.returnTypeRef.coneTypeOrNull?.substituteForDeclaration(substitutor) ?: return false
        val functionType = ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
            isCFunc = previousType.isCFunc,
            isClosureType = previousType.isClosureType,
            hasVariableLenArg = previousType.hasVariableLenArg,
            attributes = previousType.attributes,
        )
        if (restoreBodyResolveState) preBodyResolveSnapshot.restore()
        lambda.valueParameters.zip(parameterTypes).forEach { (parameter, type) ->
            parameter.replaceReturnTypeRef(type.toCfirResolvedTypeRef(parameter.returnTypeRef.source, parameter.returnTypeRef))
        }
        lambda.replaceReturnTypeRef(returnType.toCfirResolvedTypeRef(lambda.returnTypeRef.source, lambda.returnTypeRef))
        lambda.lambdaParameterShapeExpectedFunctionType = functionType
        lambda.replaceMatchingParameterFunctionType(functionType)
        lambda.replaceTypeRef(functionType.toCfirResolvedTypeRef(lambda.typeRef.source, lambda.typeRef))
        lambdaExpression.replaceConeTypeOrNull(functionType)
        return true
    }

    private fun ConeCangJieType.substituteForDeclaration(substitutor: ConeSubstitutor): ConeCangJieType =
        IdealTypeResolver.resolveIfIdeal(substitutor.substituteOrSelf(this))
}
