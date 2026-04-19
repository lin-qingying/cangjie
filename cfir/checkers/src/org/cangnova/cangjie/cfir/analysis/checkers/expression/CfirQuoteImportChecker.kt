package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQuoteExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.name.FqName

/**
 * `quote` 表达式需要导入 `std.ast`。
 *
 * 对齐 C++ DiagKind::sema_use_expr_without_import (QuoteExpr.cpp:26):
 * 未导入 `std.ast` 时 `quote {...}` 报错,提示需要的包名。
 */
object CfirQuoteImportChecker : CfirBasicExpressionChecker() {
    private val STD_AST = FqName("std.ast")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStatement) {
        if (expression !is CfirQuoteExpression) return
        val fileSymbol = context.containingFileSymbol ?: return
        val file = fileSymbol.cfir
        val imported = file.imports.any { imp ->
            val fq = imp.importedFqName ?: return@any false
            fq == STD_AST || fq.parent() == STD_AST || (imp.isAllUnder && fq == STD_AST)
        }
        if (!imported) {
            reporter.reportOn(
                source = expression.source,
                factory = CfirErrors.USE_EXPR_WITHOUT_IMPORT,
                a = STD_AST,
                b = "quote",
            )
        }
    }
}
