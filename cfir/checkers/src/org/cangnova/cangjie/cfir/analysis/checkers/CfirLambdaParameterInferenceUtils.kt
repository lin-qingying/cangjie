package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.hasOmittedLambdaParameterType
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
 * 取得源码显式写出的 lambda 参数类型。
 *
 * 类型引用可能经过多次解析和替换，并把原型保留在 delegated 链中。形状诊断追溯到
 * 最初解析自源码的显式类型，不能把后续阶段的中间类型视图当作用户标注。
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

/** 判断类型树中是否仍含未推断的 lambda 参数 placeholder（无原始类型参数的 type variable 或其派生错误）。 */
private fun ConeCangJieType.containsUninferredLambdaParameterType(): Boolean {
    if (this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null) return true
    val diagnostic = (this as? ConeErrorType)?.diagnostic?.unwrapForLambdaParameterInference()
    if (diagnostic is ConeCannotInferValueParameterType) return true
    return typeArguments.any { projection -> projection.type.containsUninferredLambdaParameterType() }
}

/** 剥离未上报重复诊断包装，取原始诊断供推断失败判定使用。 */
private fun ConeDiagnostic.unwrapForLambdaParameterInference(): ConeDiagnostic =
    (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this
