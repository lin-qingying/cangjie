package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef

/**
 * 检查 typealias 展开类型本身是否合法。
 *
 * 官方 `PreCheck::CheckTypeAliasDecl` 会先解析 RHS 类型树；只要最终 `Ty` 不正确，
 * 除内层具体错误外，还会在 RHS 根类型上报告 `sema_not_a_type`。CFIR 在解析阶段
 * 会把内层错误包装成外层 [CfirErrorTypeRef]，这里负责补齐声明侧根诊断。
 */
object CfirTypeAliasExpandedTypeChecker : CfirTypeAliasChecker() {
    private const val RECURSIVE_TYPEALIAS_PREFIX = "Recursive typealias expansion"

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeAlias) {
        val expandedTypeRef = declaration.expandedTypeRef as? CfirErrorTypeRef ?: return

        if (expandedTypeRef.diagnostic is ConeUnresolvedTypeQualifierError) return
        if (expandedTypeRef.diagnostic.reason.startsWith(RECURSIVE_TYPEALIAS_PREFIX)) return

        val qualifier = expandedTypeRef.rootTypeQualifier() ?: return
        reporter.reportOn(
            source = qualifier.source ?: expandedTypeRef.source,
            factory = CfirErrors.NOT_A_TYPE,
            a = qualifier.name.asString(),
        )
    }

    private fun CfirErrorTypeRef.rootTypeQualifier(): CfirQualifierPart? {
        return (delegatedTypeRef ?: partiallyResolvedTypeRef)
            ?.rootTypeQualifier()
    }

    private fun CfirTypeRef.rootTypeQualifier(): CfirQualifierPart? {
        return when (this) {
            is CfirUserTypeRef -> qualifier.lastOrNull()
            is CfirErrorTypeRef -> rootTypeQualifier()
            else -> null
        }
    }
}
