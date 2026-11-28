package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.shouldSkipSamePackageDeprecation
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.getDeprecation
import org.cangnova.cangjie.resolve.deprecation.DeprecationLevelValue

/**
 * 检查对已弃用属性和变量的已解析访问。
 *
 * 函数调用和类型引用分别由各自的 checker 处理；此 checker 负责变量值访问，
 * 包括属性、字段、局部变量和带默认值的形参，并统一消费 resolver 发布的符号绑定。
 */
object CfirDeprecatedValueAccessChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.calleeReference is CfirDiagnosticHolder) return

        val reference = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val callableSymbol: CfirCallableSymbol<*> = when (val symbol = reference.resolvedSymbol) {
            is CfirPropertySymbol -> symbol
            is CfirVariableSymbol<*> -> symbol
            is CfirEnumConstructorSymbol -> symbol
            else -> return
        }
        if (!callableSymbol.isBound) return

        val languageVersionSettings = context.languageVersionSettings
        val directDeprecation = callableSymbol.getDeprecation(languageVersionSettings)?.all
        val enumOwner = (callableSymbol as? CfirEnumConstructorSymbol)
            ?.let(context.session.cfirProvider::getContainingClass)
        val enumOwnerDeprecation = if (directDeprecation == null) {
            enumOwner?.getOwnDeprecation(languageVersionSettings)?.all
        } else {
            null
        }
        val deprecation = directDeprecation ?: enumOwnerDeprecation ?: return
        val isEnumOwnerDeprecation = directDeprecation == null && enumOwnerDeprecation != null
        val source = if (isEnumOwnerDeprecation) {
            (expression.explicitReceiver as? CfirQualifiedAccessExpression)?.calleeReference?.source
                ?: reference.source
                ?: expression.source
        } else {
            reference.source ?: expression.source
        } ?: return
        val isError = deprecation.deprecationLevel == DeprecationLevelValue.ERROR
        val packageName = enumOwner?.classId?.packageFqName ?: callableSymbol.callableId.packageName
        if (context.shouldSkipSamePackageDeprecation(packageName, isError)) return

        val (kind, name) = when {
            isEnumOwnerDeprecation -> "class" to requireNotNull(enumOwner).classId.shortClassName
            callableSymbol is CfirEnumConstructorSymbol -> {
                val ownerName = enumOwner?.classId?.shortClassName?.asString()
                "enum '${ownerName ?: ""}' constructor" to callableSymbol.name
            }

            callableSymbol is CfirPropertySymbol -> "property" to callableSymbol.name
            callableSymbol is CfirVariableSymbol<*> -> "variable" to callableSymbol.name
            else -> error("Only property, variable, and enum constructor symbols are handled here")
        }
        val factory = if (isError) CfirErrors.DEPRECATED_ERROR else CfirErrors.DEPRECATED_WARNING
        reporter.reportOn(
            source = source,
            factory = factory,
            a = kind,
            b = name,
            c = "",
            d = "",
        )
    }
}
