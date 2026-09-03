package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.hasInvalidGenericTypeArgument
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirStaticMemberCompleteness
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 通过 nominal qualifier 调用未实现 static 成员的检查器。
 *
 * 官方 `IsNamespaceMemberAccessLegal` 在 class/interface/typealias 等 nominal 目标上访问
 * abstract static 成员时报告 `sema_interface_call_with_unimplemented_call`；`CheckInvokeTargetHasImpl`
 * 还会在 interface static 默认实现体递归引用未实现 static 成员时，把诊断挂到外层调用。
 * 这里把两条规则放在 qualified access 层，统一覆盖 `A.f()`、`Alias.f()` 和 static property 访问。
 */
object CfirInterfaceCallWithUnimplementedCallChecker : CfirQualifiedAccessChecker() {
    /**
     * 检查 static 成员访问是否落到未实现的抽象目标，或默认实现体依赖未实现目标。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.hasInvalidGenericTypeArgument()) return
        val qualifier = expression.explicitReceiver ?: return
        val qualifierClassifier = qualifier.resolvedQualifierClassifier(context.session) ?: return

        val targetSymbol = expression.resolvedCallableSymbolOrNull() ?: return
        targetSymbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val target = targetSymbol.cfir as? CfirCallableDeclaration ?: return
        if (!target.status.isStatic) return

        if (target.status.isAbstract) {
            val (kind, name) = target.unimplementedStaticDiagnosticTarget() ?: return
            reporter.reportOn(
                source = expression.directUnimplementedStaticSource(qualifierClassifier.cfir is CfirInterface),
                factory = CfirErrors.INTERFACE_CALL_WITH_UNIMPLEMENTED_CALL,
                a = kind,
                b = name,
            )
            return
        }

        val qualifierHasMemberConflict = context.hasGenericInstantiationMemberConflict(qualifier.source)
        val qualifierType = qualifier.coneTypeOrNull ?: qualifierClassifier.constructType()
        val unimplementedDependency = CfirStaticMemberCompleteness.firstUnimplementedStaticDependency(
            finalQualifierType = qualifierType,
            invokedSymbol = targetSymbol,
            finalQualifierHasMemberConflict = qualifierHasMemberConflict,
        ) ?: return
        val (kind, name) = unimplementedDependency.cfir.unimplementedStaticDiagnosticTarget() ?: return
        reporter.reportOn(
            source = expression.indirectUnimplementedStaticSource(qualifierHasMemberConflict),
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

    /**
     * interface static 调用诊断按外层 invocation/assignment 标记；非 interface qualifier 仍标 callee。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.directUnimplementedStaticSource(isInterfaceQualifier: Boolean): CjSourceElement? =
        if (isInterfaceQualifier) outerInvocationSource() else calleeReference.source ?: source

    /**
     * 泛型实例化成员冲突会使 qualifier 上的同签名 static 实现失效；由该冲突引出的未实现调用
     * 诊断与泛型冲突共享 qualifier 范围，普通默认实现依赖仍标记外层调用/赋值。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.indirectUnimplementedStaticSource(
        qualifierHasMemberConflict: Boolean,
    ): CjSourceElement? =
        if (qualifierHasMemberConflict) explicitReceiver?.source else outerInvocationSource()

    /**
     * 属性写入时把诊断范围提升到整个赋值；普通调用/访问使用 qualified access 自身范围。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.outerInvocationSource(): CjSourceElement? {
        val assignment = context.callsOrAssignments
            .asReversed()
            .filterIsInstance<CfirAssignment>()
            .firstOrNull { assignment -> assignment.lValue === this }
        return assignment?.source ?: source
    }
}
