package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 禁止把带 C 变长参数的函数值赋给变量。
 *
 * 官方 `CheckVarDecl` 在 initializer 已被推断为 `FuncTy(isC, hasVariableLenArg)`
 * 后拒绝这种赋值。这个事实属于声明初始化 owner，不应由调用 checker 或 backend
 * 再次猜测；显式类型变量继续由普通类型匹配规则负责。
 */
object CfirCFuncVariableInitializerChecker : CfirCallableDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        val variable = declaration as? CfirVariable ?: return
        if (!variable.isTypeImplicit) return

        val initializerType = variable.initializer
            ?.coneTypeOrNull
            ?.fullyExpandedType(context.session) as? ConeFunctionType
        val targetFunction = (variable.initializer as? CfirQualifiedAccessExpression)
            ?.resolvedFunctionDeclaration()
        val hasVariableLengthCFunction =
            initializerType?.isCFunc == true && initializerType.hasVariableLenArg ||
                targetFunction?.let { it.hasVariableLenArg && (it.status.isForeign || it.status.isC) } == true
        if (!hasVariableLengthCFunction) return

        reporter.reportOn(
            source = variable.initializer?.source ?: variable.source,
            factory = CfirErrors.CFUNC_VAR_CANNOT_HAVE_VAR_PARAM,
        )
    }

    /** 从变量 initializer 的已解析引用读取 foreign/@C 函数声明。 */
    private fun CfirQualifiedAccessExpression.resolvedFunctionDeclaration(): CfirFunction? {
        val symbol = when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> return null
        } as? CfirFunctionSymbol<*> ?: return null
        return symbol.takeIf { it.isBound }?.cfir as? CfirFunction
    }
}
