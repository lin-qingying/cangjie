package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 检查变量 initializer 直接引用的 C 变长函数。
 *
 * `CheckVarDecl` 发生在函数值类型完整生成之后；CFIR 的等价 owner 是 initializer
 * 中已解析的 qualified access。这里不根据 source 文本或返回类型猜测，只消费
 * `CfirFunction.hasVariableLenArg` 与 `foreign/@C` status，并把同一规则委托给
 * 声明级 checker 的共享诊断协议。
 */
object CfirCFuncVariadicReferenceChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression) {
        if (expression is org.cangnova.cangjie.cfir.expressions.CfirFunctionCall) return
        val variable = context.containingDeclarations.asReversed()
            .mapNotNull { it.cfir as? CfirVariable }
            .firstOrNull { it.initializer === expression }
            ?: return
        if (variable.returnTypeRef !is org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef) return

        val function = expression.resolvedFunctionDeclaration() ?: return
        if (!function.hasVariableLenArg || (!function.status.isForeign && !function.status.isC)) return

        reporter.reportOn(
            source = expression.source ?: variable.source,
            factory = CfirErrors.CFUNC_VAR_CANNOT_HAVE_VAR_PARAM,
        )
    }

    /** 从已经解析的引用取得 foreign/@C 函数声明。 */
    private fun org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression.resolvedFunctionDeclaration(): CfirFunction? {
        val symbol = when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> return null
        } as? CfirFunctionSymbol<*> ?: return null
        return symbol.takeIf { it.isBound }?.cfir as? CfirFunction
    }
}
