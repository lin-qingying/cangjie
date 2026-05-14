package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
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
    return AbstractTypeChecker.isSubtypeOf(
        context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = false,
        ),
        subtypeFullyExpanded,
        supertypeFullyExpanded,
    )
}
