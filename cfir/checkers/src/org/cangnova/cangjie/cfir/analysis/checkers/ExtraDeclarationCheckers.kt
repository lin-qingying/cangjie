package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirCallableDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirDeprecatedDeclarationChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirExtendExtraChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirVArrayExtraChecker
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers

/**
 * 对齐 Kotlin `ExtraDeclarationCheckers`。
 *
 * 当前仓颉主干里已经以 “ExtraChecker” 语义命名的声明检查器统一收口到这里，
 * 由 low-level-api-cfir 根据 `DiagnosticCheckerFilter` 选择性挂载。
 */
object ExtraDeclarationCheckers : DeclarationCheckers() {
    override val callableDeclarationCheckers: Set<CfirCallableDeclarationChecker> = setOf(
        CfirVArrayExtraChecker,
        CfirDeprecatedDeclarationChecker,
    )

    override val extendCheckers: Set<CfirExtendChecker> = setOf(
        CfirExtendExtraChecker,
    )
}
