package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
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
    /**
     * 检查无显式接收者变量访问是否命中捕获变量同名遮蔽场景。
     *
     * 只有捕获外层非参数变量时才继续分析；若当前函数到目标声明之间任一函数体内有同名局部变量，
     * 则在捕获引用的首字符位置报告 warning。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.explicitReceiver != null) return

        val target = expression.resolvedVariableOrNull() ?: return
        if (target is CfirValueParameter) return

        val variableName = target.variableName
        val containingFunctions = context.containingDeclarations.asReversed().filterIsInstance<CfirFunctionSymbol<*>>().map { it.cfir }
        if (containingFunctions.size < 2) return
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

    /**
     * 从 qualified access 的引用节点解析变量声明。
     *
     * 诊断阶段可能遇到正常解析、错误恢复解析或候选解析引用，三者都需要尝试取出变量声明。
     */
    private fun CfirQualifiedAccessExpression.resolvedVariableOrNull(): CfirVariable? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirVariable
            is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirVariable
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirVariable
            else -> null
        }

    /**
     * 变量用于遮蔽比较的 callable 名称。
     *
     * 通过 symbol 的 callableId 读取，避免依赖不同变量子类的展示字段。
     */
    private val CfirVariable.variableName: Name
        get() = symbol.callableId.callableName

    /**
     * 判断目标声明是否属于当前函数自身作用域。
     *
     * 一旦目标已经在当前函数内声明，后续外层函数不再构成捕获链，应停止遮蔽 warning 分析。
     */
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

    /**
     * 判断当前函数体内是否声明了与捕获目标同名的局部变量。
     *
     * 遍历仅进入当前函数本体，不进入嵌套函数，保证遮蔽判断只覆盖同一个函数作用域。
     */
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
