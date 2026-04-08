package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 对齐官方 `mut/immutable` 核心语义的第一步：
 * 在 struct 的非 mut 成员函数中，`this` 视角下的可变操作必须被拦截。
 *
 * 这一批先只处理最稳定的两类行为：
 * 1. 赋值到当前实例字段；
 * 2. 调用当前实例上的 mut 成员函数。
 */
object CfirImmutableFunctionCannotModifyFieldChecker : CfirAssignmentChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        context.currentImmutableStructFunction() ?: return

        val lValue = expression.lValue as? CfirQualifiedAccessExpression ?: return
        if (!lValue.isCurrentStructReceiverAccess()) return

        val fieldSymbol = lValue.resolvedFieldSymbolOrNull() ?: return
        val field = fieldSymbol.takeIf { it.isBound }?.cfir as? CfirFieldVariable ?: return
        if (!field.isVar) return

        reporter.reportOn(
            source = lValue.calleeReference.source ?: lValue.source ?: expression.source,
            factory = CfirErrors.CANNOT_MODIFY_VAR,
            a = field.name,
        )
    }
}

object CfirImmutableFunctionCannotAccessMutableFunctionChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val currentFunction = context.currentImmutableStructFunction() ?: return
        if (!expression.isCurrentStructReceiverAccess()) return

        val targetSymbol = expression.resolvedFunctionSymbolOrNull() ?: return
        val targetFunction = targetSymbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return
        if (!targetFunction.status.isMut) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
            a = currentFunction.name,
            b = targetFunction.name,
        )
    }
}

private fun CheckerContext.currentImmutableStructFunction(): CfirNamedFunction? {
    val function = findClosestDeclaration<CfirNamedFunction>() ?: return null
    if (function.status.isMut) return null
    if (findClosestDeclaration<CfirStruct>() == null) return null
    return function
}

private fun CfirQualifiedAccessExpression.isCurrentStructReceiverAccess(): Boolean {
    return explicitReceiver == null || explicitReceiver is CfirThisReceiverExpression
}

private fun CfirQualifiedAccessExpression.resolvedFieldSymbolOrNull(): CfirFieldVariableSymbol? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFieldVariableSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFieldVariableSymbol
        else -> null
    }
}

private fun CfirFunctionCall.resolvedFunctionSymbolOrNull(): CfirFunctionSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFunctionSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFunctionSymbol<*>
        else -> null
    }
}
