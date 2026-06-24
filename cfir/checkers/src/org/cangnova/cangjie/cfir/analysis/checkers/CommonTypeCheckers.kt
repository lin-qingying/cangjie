package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirTypeProjectionModifierChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirUpperBoundViolatedTypeChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirVArrayElementTypeChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirVArraySizeLiteralChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckers

/** CFIR 默认类型引用 checker 注册表，汇总普通类型引用和已解析类型引用阶段的检查器。 */
object CommonTypeCheckers : TypeCheckers() {
    /** 在类型解析前后都可基于语法类型引用执行的检查器集合。 */
    override val typeRefCheckers
        get() = setOf(
            CfirTypeProjectionModifierChecker,
            CfirVArraySizeLiteralChecker,
        )

    /** 依赖 `CfirResolvedTypeRef` 中 Cone 类型结果的检查器集合。 */
    override val resolvedTypeRefCheckers
        get() = setOf(
            CfirUpperBoundViolatedTypeChecker,
            CfirVArrayElementTypeChecker,
        )
}
