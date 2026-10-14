package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.shouldSkipSamePackageDeprecation
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.toClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue
import org.cangnova.cangjie.source.CjSourceElement

/**
 * @Deprecated 类型引用检查器
 *
 * 对齐 C++ `TypeCheckReference.cpp` `CheckUsageOfDeprecated`：
 * 当通过类型引用（如 `let mtx: ReentrantMutex`）使用被 `@Deprecated` 标记的 class/struct/enum 时，
 * 按其严格级别报告 warning/error。弃用信息通过声明上的 `deprecationsProvider` 获取。
 */
object CfirDeprecatedTypeRefChecker : CfirResolvedTypeRefChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        val symbol = typeRef.coneType.toClassLikeSymbol(context.session) ?: return
        val deprecation = symbol.getOwnDeprecation(context.languageVersionSettings)?.all ?: return
        val source = typeRef.deprecationDiagnosticSource() ?: return

        val isError = deprecation.deprecationLevel == DeprecationLevelValue.ERROR
        if (context.shouldSkipSamePackageDeprecation(symbol.classId.packageFqName, isError)) return
        val factory = if (isError) CfirErrors.DEPRECATED_ERROR else CfirErrors.DEPRECATED_WARNING
        val declName: Name = symbol.classId.shortClassName

        reporter.reportOn(
            source = source,
            factory = factory,
            a = "class",
            b = declName,
            c = "",
            d = "",
        )
    }
}

/** 类型弃用归属于源码中实际书写的最终类型名；没有该名称 token 时不报告类型弃用。 */
private fun CfirResolvedTypeRef.deprecationDiagnosticSource(): CjSourceElement? =
    delegatedUserTypeRef()?.qualifier?.lastOrNull()?.source

/** 沿 resolved wrapper 找到实际承载源码限定名链的 user type ref。 */
private fun CfirResolvedTypeRef.delegatedUserTypeRef(): CfirUserTypeRef? {
    var current: CfirTypeRef? = delegatedTypeRef
    while (current is CfirResolvedTypeRef) {
        current = current.delegatedTypeRef
    }
    return current as? CfirUserTypeRef
}
