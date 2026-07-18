package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeIdealFloatLiteralType
import org.cangnova.cangjie.cfir.types.ConeIdealIntLiteralType
import org.cangnova.cangjie.cfir.types.ConeTypePreparator
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.AbstractTypeRefiner
import org.cangnova.cangjie.type.TypeCheckerState

/**
 * 官方 lambda 参数标注规则。
 *
 * `ChkLamParamTys` 对每个目标函数参数执行
 * `IsSubtype(paramTy, annotatedTy, false, false)`；这里保留同一条子类型关系，
 * 并关闭值类型到接口的自动装箱路径。
 */
fun isLambdaTargetParameterSubtypeOfAnnotation(
    session: CfirSession,
    targetParameterType: ConeCangJieType,
    annotatedParameterType: ConeCangJieType,
): Boolean {
    lambdaPrimitiveParameterSubtypeOfAnnotation(targetParameterType, annotatedParameterType)?.let { return it }

    val state = TypeCheckerState(
        isErrorTypeEqualsToAnything = true,
        isStubTypeEqualsToAnything = false,
        allowedTypeVariable = false,
        allowImplicitBoxing = false,
        typeSystemContext = session.typeContext,
        cangjieTypePreparator = ConeTypePreparator(session),
        cangjieTypeRefiner = AbstractTypeRefiner.Default,
    )
    return AbstractTypeChecker.isSubtypeOf(state, targetParameterType, annotatedParameterType)
}

/**
 * 官方 `TypeManager::IsPrimitiveSubtype` 在 lambda 参数标注检查中只接受：
 * - IdealInt/IdealFloat 到对应数值族；
 * - 两个 primitive 的 kind 完全一致。
 */
private fun lambdaPrimitiveParameterSubtypeOfAnnotation(
    targetParameterType: ConeCangJieType,
    annotatedParameterType: ConeCangJieType,
): Boolean? {
    val targetKind = targetParameterType.primitiveKindForLambdaParameterRule()
    val annotatedKind = annotatedParameterType.primitiveKindForLambdaParameterRule()
    if (targetKind == null && annotatedKind == null) return null

    if (targetKind == PrimitiveTypeKind.NOTHING) return true
    if (targetKind == PrimitiveTypeKind.IDEAL_INT) return annotatedKind?.isInteger == true
    if (targetKind == PrimitiveTypeKind.IDEAL_FLOAT) return annotatedKind?.isFloat == true

    return targetKind != null && annotatedKind != null && targetKind == annotatedKind
}

private fun ConeCangJieType.primitiveKindForLambdaParameterRule(): PrimitiveTypeKind? =
    when (this) {
        is ConeIdealIntLiteralType -> PrimitiveTypeKind.IDEAL_INT
        is ConeIdealFloatLiteralType -> PrimitiveTypeKind.IDEAL_FLOAT
        is ConePrimitiveType -> kind
        else -> expandedClassIdOrPrimitiveClassId?.toPrimitiveTypeKindOrNull()
    }
