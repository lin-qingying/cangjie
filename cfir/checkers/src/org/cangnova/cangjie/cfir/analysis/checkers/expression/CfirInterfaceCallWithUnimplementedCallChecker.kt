package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.name.Name

/**
 * 通过 nominal qualifier 调用未实现 static 成员的检查器。
 *
 * 官方 `IsNamespaceMemberAccessLegal` 在 class/interface/typealias 等 nominal 目标上访问
 * abstract static 成员时报告 `sema_interface_call_with_unimplemented_call`；这里把该规则放在
 * qualified access 层，统一覆盖 `A.f()`、`Alias.f()` 和 static property 访问。
 */
object CfirInterfaceCallWithUnimplementedCallChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查 static 成员访问是否落到未实现的抽象目标。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val qualifier = expression.explicitReceiver ?: return
        qualifier.resolvedQualifierClassifier(context.session) ?: return

        val targetSymbol = expression.resolvedCallableSymbolOrNull() ?: return
        targetSymbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val target = targetSymbol.cfir as? CfirCallableDeclaration ?: return
        if (!target.status.isStatic || !target.status.isAbstract) return

        val (kind, name) = target.unimplementedStaticDiagnosticTarget() ?: return
        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL,
            a = kind,
            b = name,
        )
    }

    /**
     * 解析 qualified access 背后的 callable symbol，包含错误引用中保留的单候选。
     */
    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            is CfirErrorNamedReference ->
                (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }

    /**
     * 将目标声明转换成官方诊断需要的成员种类与名称。
     */
    private fun CfirCallableDeclaration.unimplementedStaticDiagnosticTarget(): Pair<String, Name>? =
        when (this) {
            is CfirNamedFunction -> "function" to name
            is CfirProperty -> "property" to name
            else -> null
        }
}
