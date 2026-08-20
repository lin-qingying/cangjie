package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

/**
 * 函数值引用合法性检查。
 *
 * 这里处理的是“函数名被当作值使用，而不是直接调用”的场景：
 * - `mut` 成员函数不能单独作为引用暴露；
 * - `unsafe` 函数必须通过调用点进入，不能只取其名字。
 *
 * 说明：
 * - 普通函数调用会走 `CfirFunctionCall`，这里显式跳过，避免与调用语义重叠。
 * - 该规则只基于已经解析完成的函数符号，不做额外候选决议。
 */
object CfirFunctionReferenceLegalityChecker : CfirQualifiedAccessChecker() {
    /** 检查函数名作为值访问时是否违反 mut/unsafe 或 enum 构造器参数规则。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirFunctionCall) return

        val targetSymbol = expression.resolvedCallableSymbolOrNull()
        if (targetSymbol == null) {
            val recoveredMutFunction = expression.declaredUpperBoundMutFunctionOrNull() ?: return
            val diagnosticSource = expression.calleeReference.source ?: expression.source ?: return
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.USE_MUTABLE_FUNC_ALONE,
                a = recoveredMutFunction.name,
            )
            return
        }

        targetSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val targetDeclaration = targetSymbol.takeIf { it.isBound }?.cfir ?: return
        val diagnosticSource = expression.calleeReference.source ?: expression.source ?: return

        if (targetDeclaration is CfirEnumConstructor && targetDeclaration.valueParameters.isNotEmpty()) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.ENUM_CONSTRUCTOR_WITH_PARAM_MUST_HAVE_ARGS,
                a = targetDeclaration.name,
            )
            return
        }

        val targetFunction = targetDeclaration as? CfirNamedFunction ?: return

        if (targetFunction.status.isMut) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.USE_MUTABLE_FUNC_ALONE,
                a = targetFunction.name,
            )
        }

        if (targetFunction.status.isUnsafe) {
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.UNSAFE_FUNC_CAN_ONLY_BE_CALLED,
            )
        }
    }

    /**
     * 从限定访问的 calleeReference 中提取 callable symbol。
     *
     * enum 构造器在 CFIR 中是独立的 [CfirCallableSymbol]，并不继承
     * [CfirFunctionSymbol]；因此函数引用合法性检查必须先覆盖整个 callable
     * 空间，再在普通函数规则中收窄到 [CfirNamedFunction]。
     */
    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }
    }
}
