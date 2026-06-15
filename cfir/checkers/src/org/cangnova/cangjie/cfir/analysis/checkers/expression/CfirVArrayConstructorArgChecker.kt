package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.CfirVArraySizeLiteralUtils
import org.cangnova.cangjie.cfir.analysis.checkers.findUnsupportedVArrayElementType
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.source.CjSourceElement

/**
 * VArray 构造器检查。
 *
 * 对齐 C++ DiagKind::sema_varray_args_number_mismatch:
 * `VArray<T, N>(...)` 构造器只接受一个参数（初始化 lambda 或 repeat/item）。
 *
 * 直接构造语法 `VArray<T, $N>(...)` 在 CFIR 中被建模为 synthetic function call，
 * 其元素类型 `T` 不再处于完整的 `CfirVArrayTypeRef` 内，因此这里补上与
 * `CheckVArrayType` 相同的元素类型限制入口。
 */
object CfirVArrayConstructorArgChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        if (!expression.isBuiltinVArrayConstructorCall()) return

        expression.checkSizeLiteral()
        expression.checkExplicitElementType()

        val argCount = expression.argumentList.arguments.size
        if (argCount == 1) return
        reporter.reportOn(
            source = expression.argumentList.source ?: expression.source,
            factory = CfirErrors.VARRAY_ARGS_NUMBER_MISMATCH,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirFunctionCall.checkSizeLiteral() {
        val sizeLiteral = varraySizeLiteral ?: return
        val parsed = CfirVArraySizeLiteralUtils.overflowingSizeLiteral(sizeLiteral) ?: return
        reporter.reportOn(
            source = CfirVArraySizeLiteralUtils.sizeLiteralDiagnosticSource(source, sizeLiteral),
            factory = CfirErrors.LITERAL_NUMERIC_OVERFLOW,
            a = parsed.originalText,
            b = CfirVArraySizeLiteralUtils.targetType,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirFunctionCall.checkExplicitElementType() {
        val elementTypeRef = typeArguments.firstOrNull() as? CfirResolvedTypeRef ?: return
        val unsupportedType = findUnsupportedVArrayElementType(elementTypeRef.coneType) ?: return
        reporter.reportOn(
            source = elementTypeRef.originalSource() ?: source,
            factory = CfirErrors.VARRAY_ARG_TYPE_WITH_REFTYPE,
            a = unsupportedType,
        )
    }

    context(context: CheckerContext)
    private fun CfirFunctionCall.isBuiltinVArrayConstructorCall(): Boolean {
        if (varraySizeLiteral != null) return true

        if (coneTypeOrNull?.fullyExpandedType(context.session) !is ConeVArrayType) return false

        val symbol = when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFunctionSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFunctionSymbol<*>
            else -> null
        } ?: return false

        return symbol.takeIf { it.isBound }
            ?.cfir
            ?.origin == CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor
    }

    private fun CfirTypeRef.originalSource(): CjSourceElement? =
        when (this) {
            is CfirResolvedTypeRef -> delegatedTypeRef?.originalSource() ?: source
            else -> source
        }
}
