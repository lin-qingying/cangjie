package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * 裸泛型 classifier 被当作值或限定符使用时，需要落到专门的 generic 诊断，
 * 而不是继续等待后续阶段退化成普通 unresolved。
 */
object CfirGenericBareClassifierAccessChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.typeArguments.isNotEmpty()) return

        val resolvedReference = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val resolvedSymbol = resolvedReference.resolvedSymbol as? CfirClassLikeSymbol<*> ?: return
        if (resolvedSymbol.cfir.typeParameters.isEmpty()) return

        reporter.reportOn(
            source = resolvedReference.source ?: expression.source,
            factory = CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT,
            a = resolvedSymbol.classId.shortClassName,
        )
    }
}
