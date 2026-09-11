package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic

/** 表达式自身的可见 Join 失败；与函数返回类型推断失败分开，避免上层声明重复报告。 */
class ConeIncompatibleExpressionTypesError(
    val constructName: String,
    val branchTypes: List<ConeCangJieType>,
) : ConeDiagnostic {
    override val reason: String = "The types ${branchTypes.joinToString()} do not have the smallest common supertype in $constructName"
}
