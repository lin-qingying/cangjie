package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.isLambdaParameterTypeOmitted
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.source.CjFakeSourceElementKind

/**
 * Lambda 参数推断失败的共享判定。
 *
 * 官方 `SynLamExpr` 会先把省略类型的参数作为 placeholder 参与 body 推断；
 * 若最终仍有任一省略参数无法解出，只在第一个省略参数上报告注解缺失，
 * body 中由同一 placeholder 派生的二级错误由各 checker 共享抑制。
 */
internal fun CfirAnonymousFunction.firstOmittedLambdaParameterForInferenceFailure(): CfirValueParameter? {
    if (!isLambda || !hasExplicitParameterList) return null
    if (!valueParameters.any { it.hasOmittedLambdaParameterType() && it.hasUninferredLambdaParameterType() }) {
        return null
    }
    return valueParameters.firstOrNull { it.hasOmittedLambdaParameterType() }
}

/**
 * 是否存在仍未解出的省略类型 lambda 参数。
 */
internal fun CfirAnonymousFunction.hasUninferredOmittedLambdaParameterType(): Boolean =
    firstOmittedLambdaParameterForInferenceFailure() != null

/**
 * 判断参数类型是否来自源码省略，而不是显式类型标注。
 */
internal fun CfirValueParameter.hasOmittedLambdaParameterType(): Boolean {
    if (isLambdaParameterTypeOmitted == true) return true
    if (returnTypeRef is CfirImplicitTypeRef) return true
    return returnTypeRef.source?.kind == CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter
}

/**
 * 取得源码显式写出的 lambda 参数类型。
 *
 * Completion 会把目标函数参数类型写回 lambda header，并把旧 type-ref 挂到 delegated
 * 链上；重载和 PCLA 场景可能多次写回。形状诊断必须追溯到最初解析自源码的显式类型，
 * 不能把最近一次写回的 expected type 当作用户标注。
 */
internal fun CfirValueParameter.explicitLambdaParameterType(): ConeCangJieType? {
    if (hasOmittedLambdaParameterType()) return null

    var current = returnTypeRef
    var explicitType: ConeCangJieType? = null
    while (current is CfirResolvedTypeRef) {
        explicitType = current.coneType
        current = current.delegatedTypeRef ?: break
    }
    return explicitType
}

/**
 * 判断参数类型是否仍含无法作为真实参数类型发布的 placeholder。
 */
private fun CfirValueParameter.hasUninferredLambdaParameterType(): Boolean {
    val typeRef = returnTypeRef
    return when {
        typeRef is CfirImplicitTypeRef -> true
        typeRef is CfirErrorTypeRef ->
            typeRef.diagnostic.unwrapForLambdaParameterInference() is ConeCannotInferValueParameterType
        typeRef is CfirResolvedTypeRef -> typeRef.coneType.containsUninferredLambdaParameterType()
        else -> false
    }
}

private fun ConeCangJieType.containsUninferredLambdaParameterType(): Boolean {
    if (this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null) return true
    val diagnostic = (this as? ConeErrorType)?.diagnostic?.unwrapForLambdaParameterInference()
    if (diagnostic is ConeCannotInferValueParameterType) return true
    return typeArguments.any { projection -> projection.type.containsUninferredLambdaParameterType() }
}

private fun ConeDiagnostic.unwrapForLambdaParameterInference(): ConeDiagnostic =
    (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this
