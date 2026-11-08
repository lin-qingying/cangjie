package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCallableDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirBasicDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDceUnusedDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirFileChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirUnusedExpressionChecker
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

    /**
     * 额外声明检查中面向 callable 声明的 checker 集合。
     *
     * 这里只能收录**仅由扩展集合提供**的 checker：`EXTENDED_AND_COMMON_CHECKERS` 会按
     * common / extended 两个 filter 分别派发同一批声明，若一个 checker 同时出现在
     * [CommonDeclarationCheckers.callableDeclarationCheckers] 与这里，它的每条诊断都会被报告两次
     * （真实项目 mariadb 的 `DEPRECATION_OVERRIDE_WARNING` 因此出现 5 对重复）。
     * `CfirVArrayExtraChecker` / `CfirDeprecatedDeclarationChecker` 已属 common 集合，不再重复收录。
     */
    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker> = emptySet()

    /** 额外声明检查中面向文件级 DCE warning 的 checker 集合。 */
    override val fileCheckers: Set<CfirFileChecker> = setOf(
        CfirDceUnusedDeclarationChecker,
    )

    /**
     * 额外声明检查中面向 `extend` 声明的 checker 集合。
     *
     * 与 callable 集合同理：`CfirExtendExtraChecker` 已由 common 集合提供，这里不再重复收录，
     * 否则 `EXTENDED_AND_COMMON_CHECKERS` 会把同一条 extend 诊断报告两次。
     */
    override val extendCheckers: Set<CfirExtendChecker> = emptySet()
}
