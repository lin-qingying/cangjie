package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCallableDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDceUnusedDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeprecatedDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendExtraChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFileChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirUnusedExpressionChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirVArrayExtraChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers

/**
 * 对齐 Kotlin `ExtraDeclarationCheckers`。
 *
 * 当前仓颉主干里已经以 “ExtraChecker” 语义命名的声明检查器统一收口到这里，
 * 由 low-level-api-cfir 根据 `DiagnosticCheckerFilter` 选择性挂载。
 */
object ExtraDeclarationCheckers : DeclarationCheckers() {
    /** 额外声明检查中面向所有声明节点的 lint/DCE 风格 checker 集合。 */
    override val basicDeclarationCheckers: Set<CfirBasicDeclarationChecker> = setOf(
        CfirUnusedExpressionChecker,
    )

    /** 额外声明检查中面向 callable 声明的 checker 集合。 */
    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker> = setOf(
        CfirVArrayExtraChecker,
        CfirDeprecatedDeclarationChecker,
    )

    /** 额外声明检查中面向文件级 DCE warning 的 checker 集合。 */
    override val fileCheckers: Set<CfirFileChecker> = setOf(
        CfirDceUnusedDeclarationChecker,
    )

    /** 额外声明检查中面向 `extend` 声明的 checker 集合。 */
    override val extendCheckers: Set<CfirExtendChecker> = setOf(
        CfirExtendExtraChecker,
    )
}
