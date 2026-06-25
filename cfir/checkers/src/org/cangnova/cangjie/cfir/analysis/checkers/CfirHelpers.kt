package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 类型不匹配诊断专用的子类型判断。
 *
 * 对齐 Kotlin `FirHelpers.isSubtypeForTypeMismatch`：诊断阶段先展开类型别名，
 * 再以“错误类型可匹配任意类型、stub 类型不可匹配任意类型”的状态调用类型检查器。
 */
fun isSubtypeForTypeMismatch(
    session: CfirSession,
    context: ConeInferenceContext,
    subtype: ConeCangJieType,
    supertype: ConeCangJieType,
): Boolean {
    val subtypeFullyExpanded = subtype.fullyExpandedType(session)
    val supertypeFullyExpanded = supertype.fullyExpandedType(session)
    // `This` 返回类型要求实际返回值也保持 `This` 视图；普通 class 实例即使 ClassId 相同也不能替代。
    if (
        supertypeFullyExpanded is ConeClassLikeType &&
        supertypeFullyExpanded.isThisType &&
        (subtypeFullyExpanded !is ConeClassLikeType || !subtypeFullyExpanded.isThisType)
    ) {
        return false
    }
    return AbstractTypeChecker.isSubtypeOf(
        context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = false,
        ),
        subtypeFullyExpanded,
        supertypeFullyExpanded,
    )
}

/**
 * 根据期望返回类型选择返回值类型不匹配诊断工厂。
 *
 * 普通返回类型使用 [CfirErrors.RETURN_TYPE_MISMATCH]；当期望类型是 `This`
 * 时，官方语义要求使用通用 [CfirErrors.TYPE_MISMATCH]，避免把 `This`
 * 约束降级为普通类实例返回类型不匹配。
 */
fun diagnosticFactoryForReturnTypeMismatch(
    session: CfirSession,
    expectedType: ConeCangJieType,
): CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean> {
    val expandedExpectedType = expectedType.fullyExpandedType(session)
    // 官方 `This` 返回约束按通用类型不匹配报告，而不是普通 return-type mismatch。
    return if (expandedExpectedType is ConeClassLikeType && expandedExpectedType.isThisType) {
        CfirErrors.TYPE_MISMATCH
    } else {
        CfirErrors.RETURN_TYPE_MISMATCH
    }
}
