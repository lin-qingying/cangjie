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
import org.cangnova.cangjie.source.psi

/**
 * @Deprecated 语义检查器
 *
 * 对齐 C++ DeclAttributeChecker.cpp:
 * - 调用标记了 @Deprecated 的声明时发出警告
 */
object CfirDeprecatedCallChecker : CfirFunctionCallChecker() {
    /** 检查函数调用目标是否被 `@Deprecated` 标记，并按注解等级报告 warning/error。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val symbol = expression.resolvedCallableSymbol() ?: return
        val declaration = symbol.takeIf { it.isBound }?.cfir ?: return

        val deprecatedAnnotation = findDeprecatedAnnotation(declaration) ?: return
        val source = expression.calleeReference.source ?: expression.source ?: return
        val declName = extractDeclarationName(declaration)

        // 检查 @Deprecated 注解的 level 参数来区分 error/warning
        val isError = isDeprecatedError(deprecatedAnnotation)
        val factory = if (isError) CfirErrors.DEPRECATED_ERROR else CfirErrors.DEPRECATED_WARNING

        reporter.reportOn(
            source = source,
            factory = factory,
            a = "function",
            b = declName,
            c = "",
            d = "",
        )
    }

    /** 从函数调用 calleeReference 中提取已经解析到的 callable symbol。 */
    private fun CfirFunctionCall.resolvedCallableSymbol(): CfirCallableSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }
    }

    /** 在声明注解中查找解析为 `Deprecated` class-like 的注解。 */
    private fun findDeprecatedAnnotation(declaration: CfirDeclaration): CfirAnnotation? {
        return declaration.annotations.firstOrNull { annotation ->
            val resolvedType = (annotation.typeRef as? CfirResolvedTypeRef)?.coneType
            resolvedType is ConeClassLikeType &&
                resolvedType.classId.shortClassName.asString() == "Deprecated"
        }
    }

    /** 提取用于弃用诊断展示的声明名。 */
    private fun extractDeclarationName(declaration: CfirDeclaration): Name {
        return when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> declaration.name
            is org.cangnova.cangjie.cfir.declarations.CfirProperty -> declaration.name
            is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> declaration.name
            else -> Name.identifier("<unknown>")
        }
    }

    /**
     * 判断 @Deprecated 注解是否指定了 error 级别。
     *
     * 对齐 C++ DiagKind::sema_deprecated_error vs sema_deprecated_warning:
     * @Deprecated 注解的 level 参数为 "ERROR" 时为 error 级别。
     */
    private fun isDeprecatedError(annotation: CfirAnnotation): Boolean {
        for (arg in annotation.arguments) {
            val psi = arg.source?.psi
            val text = psi?.text ?: continue
            if (text.contains("ERROR") || text.contains("error")) return true
        }
        return false
    }
}
