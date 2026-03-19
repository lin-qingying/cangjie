package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * 属性初始化类型检查器。
 * 检查类或结构体成员属性 `var p: T = expr` 中 `expr` 的类型是否为 `T` 的子类型。
 */
object CfirPropertyInitializerTypeMismatchChecker : CfirPropertyChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirProperty) {
        val source = declaration.source as? AbstractCjSourceElement ?: return
        val expectedTypeRef = declaration.returnTypeRef as? CfirResolvedTypeRef ?: return
        val actualType = declaration.initializer?.coneTypeOrNull ?: return
        checkTypeMismatch(
            expectedType = expectedTypeRef.coneType,
            actualType = actualType,
            source = source,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }
}
