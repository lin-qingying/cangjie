package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name

/**
 * 捕获变量与中间作用域同名变量的 warning 检查。
 *
 * 对齐官方 Cangjie `TypeCheckerImpl::CheckWarningOfCaptureVariable`：
 * 当前函数/闭包捕获外层非参数变量时，如果从当前函数向外到目标变量所在作用域之间，
 * 任一函数作用域内存在同名变量声明，则在捕获引用处报告 warning。
 */
object CfirCaptureHasShadowVariableChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.explicitReceiver != null) return

        val target = expression.resolvedVariableOrNull() ?: return
        if (target is CfirValueParameter) return

        val variableName = target.variableName
        val containingFunctions = context.containingDeclarations.asReversed().filterIsInstance<CfirFunction>()
        for (function in containingFunctions) {
            if (function.containsDeclarationInOwnScope(target)) return
            if (!function.hasShadowVariableDeclaration(variableName, target)) continue

            reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                    ?: expression.source?.firstCharacterDiagnosticSource(),
                factory = CfirErrors.CAPTURE_HAS_SHADOW_VARIABLE,
                a = variableName,
            )
            return
        }
    }

    private fun CfirQualifiedAccessExpression.resolvedVariableOrNull(): CfirVariable? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirVariable
            is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirVariable
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirVariable
            else -> null
        }

    private val CfirVariable.variableName: Name
        get() = symbol.callableId.callableName

    private fun CfirFunction.containsDeclarationInOwnScope(target: CfirDeclaration): Boolean {
        if (valueParameters.any { it === target }) return true

        var found = false
        body?.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (found) return
                if (element === target) {
                    found = true
                    return
                }
                element.acceptChildren(this)
            }

            override fun visitFunction(function: CfirFunction) {
                if (function === this@containsDeclarationInOwnScope) {
                    function.acceptChildren(this)
                }
            }
        })
        return found
    }

    private fun CfirFunction.hasShadowVariableDeclaration(name: Name, target: CfirDeclaration): Boolean {
        var found = false
        body?.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (found) return
                if (element is CfirVariable &&
                    element !is CfirValueParameter &&
                    element !== target &&
                    element.variableName == name
                ) {
                    found = true
                    return
                }
                element.acceptChildren(this)
            }

            override fun visitFunction(function: CfirFunction) {
                if (function === this@hasShadowVariableDeclaration) {
                    function.acceptChildren(this)
                }
            }
        })
        return found
    }
}
