package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.text

/**
 * Pattern variable initializer type mismatch checker.
 *
 * 普通表达式初始化器由这里比较声明类型与初始化器类型。
 * 官方仓颉编译器把这类 `sema_mismatched_types` 锚定在初始化表达式，
 * 因此这里与字段初始化器保持同一诊断表面：报告表达式上的 `TYPE_MISMATCH`。
 */
object CfirPatternVariableInitializerTypeMismatchChecker : CfirPatternVariableChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirPatternVariable) {
        val source = declaration.source as? AbstractCjSourceElement ?: return

        val expectedType = (declaration.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val initializer = declaration.initializer?.takeIf { it !is CfirErrorExpression } ?: return
        if (initializer.hasResolutionDiagnostic()) return
        if (initializer.isBareEnumConstructorAccess()) return
        val actualType = initializer.coneTypeOrNull ?: return
        val initializerSource = initializer.source as? AbstractCjSourceElement
        val isEnumConstructorInitializer = initializer.isEnumConstructorAccess()

        checkTypeMismatch(
            expectedType = expectedType,
            actualType = actualType,
            source = if (isEnumConstructorInitializer) initializerSource ?: source else source,
            preferredSpecializedSource = initializerSource,
            diagnosticFactory = CfirErrors.TYPE_MISMATCH,
        )
    }
}

private fun CfirExpression.hasResolutionDiagnostic(): Boolean {
    return when (this) {
        is CfirNamedAccessExpression -> calleeReference is CfirDiagnosticHolder
        is CfirQualifiedAccessExpression -> calleeReference is CfirDiagnosticHolder
        else -> false
    }
}

private fun CfirExpression.isBareEnumConstructorAccess(): Boolean {
    val access = when (this) {
        is CfirNamedAccessExpression -> this
        is CfirQualifiedAccessExpression -> this
        else -> return false
    }
    if (access.hasExplicitTypeArgumentsInSource()) return false

    val symbol = access.calleeReference.enumConstructorSymbol() ?: return false
    return symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
}

private fun CfirExpression.isEnumConstructorAccess(): Boolean {
    val access = when (this) {
        is CfirNamedAccessExpression -> this
        is CfirQualifiedAccessExpression -> this
        else -> return false
    }
    val symbol = access.calleeReference.enumConstructorSymbol() ?: return false
    return symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
}

private fun CfirQualifiedAccessExpression.hasExplicitTypeArgumentsInSource(): Boolean =
    source.text?.contains('<') == true

private fun CfirReference.enumConstructorSymbol() = when (this) {
    is CfirResolvedAppliedCallableReference -> resolvedSymbol
    is CfirResolvedNamedReference -> resolvedSymbol
    is CfirNamedReferenceWithCandidateBase -> candidateSymbol
    else -> null
}
