package org.cangnova.cangjie.cfir.resolve.calls

import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.hasOmittedLambdaParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeParamCountMismatchError
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.diagnostic.LambdaParameterCountMismatch
import org.cangnova.cangjie.cfir.diagnostic.LambdaParameterTypeMismatch
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealFloatLiteralType
import org.cangnova.cangjie.cfir.types.ConeIdealIntLiteralType
import org.cangnova.cangjie.cfir.types.ConeTypePreparator
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.AbstractTypeRefiner
import org.cangnova.cangjie.type.TypeCheckerState
import org.cangnova.cangjie.type.model.contains

/** 头部首错及其后尚未从目标类型检查的参数范围；正文仍须正常分析独立错误。 */
data class CfirLambdaParameterTypingFailure(
    /** 官方 ChkLamParamTys 退出后第一个未检查的参数下标。 */
    val firstUncheckedParameterIndex: Int,
    /** 由 lambda 头部诊断路径报告的原始错误。 */
    val diagnostic: ResolutionDiagnostic,
    /** 其后省略参数沿用首错，不能继续填入未经检查的目标类型。 */
    val parameterErrorType: ConeErrorType,
)

/**
 * 对位官方 ChkLamParamTys：先检查参数个数，再依源码顺序检查显式标注，首错即停止。
 * 有省略参数时保留头部错误及已检查的前缀；全显式 lambda 由完整函数类型检查处理。
 */
fun CfirAnonymousFunction.lambdaParameterTypingFailure(
    session: CfirSession,
    expectedFunctionType: ConeFunctionType,
): CfirLambdaParameterTypingFailure? {
    if (valueParameters.none { it.hasOmittedLambdaParameterType() }) return null
    if (valueParameters.size != expectedFunctionType.parameterTypes.size) {
        val diagnostic = LambdaParameterCountMismatch(this, expectedFunctionType.parameterTypes.size, valueParameters.size)
        return CfirLambdaParameterTypingFailure(
            firstUncheckedParameterIndex = 0,
            diagnostic = diagnostic,
            parameterErrorType = ConeErrorType(
                ConeUnreportedDuplicateDiagnostic(ConeParamCountMismatchError(diagnostic.expectedCount, diagnostic.actualCount)),
            ),
        )
    }
    valueParameters.forEachIndexed { index, parameter ->
        if (parameter.hasOmittedLambdaParameterType()) return@forEachIndexed
        val actualType = parameter.returnTypeRef.coneTypeOrNull ?: return@forEachIndexed
        val expectedType = expectedFunctionType.parameterTypes[index]
        if (actualType is ConeErrorType || expectedType is ConeErrorType) return@forEachIndexed
        // 未固定的目标还不能决定头部是否失败；补全得到目标后使用同一规则重查。
        if (with(session.typeContext) { expectedType.contains { it is ConeTypeVariableType } }) return@forEachIndexed
        if (!isLambdaTargetParameterSubtypeOfAnnotation(session, expectedType, actualType)) {
            return CfirLambdaParameterTypingFailure(
                firstUncheckedParameterIndex = index + 1,
                diagnostic = LambdaParameterTypeMismatch(this, parameter, expectedType, actualType),
                parameterErrorType = ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeTypeMismatchError(expectedType, actualType))),
            )
        }
    }
    return null
}

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
