package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.name.Name

/**
 * @Deprecated 语义检查器
 *
 * 对齐 C++ DeclAttributeChecker.cpp:
 * - 调用标记了 @Deprecated 的声明时发出警告
 */
object CfirDeprecatedCallChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val symbol = expression.resolvedCallableSymbol() ?: return
        val declaration = symbol.takeIf { it.isBound }?.cfir ?: return

        val deprecatedAnnotation = findDeprecatedAnnotation(declaration) ?: return
        val source = expression.calleeReference.source ?: expression.source ?: return
        val declName = extractDeclarationName(declaration)

        reporter.reportOn(
            source = source,
            factory = CfirErrors.DEPRECATED_WARNING,
            a = "function",
            b = declName,
            c = "",
            d = "",
        )
    }

    private fun CfirFunctionCall.resolvedCallableSymbol(): CfirCallableSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }
    }

    private fun findDeprecatedAnnotation(declaration: CfirDeclaration): CfirAnnotation? {
        return declaration.annotations.firstOrNull { annotation ->
            val resolvedType = (annotation.typeRef as? CfirResolvedTypeRef)?.coneType
            resolvedType is ConeClassLikeType &&
                resolvedType.classId.shortClassName.asString() == "Deprecated"
        }
    }

    private fun extractDeclarationName(declaration: CfirDeclaration): Name {
        return when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> declaration.name
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.name
            is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> declaration.name
            else -> Name.identifier("<unknown>")
        }
    }
}
