package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.specificTypeMismatchDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 统一检查声明初始化器类型是否兼容期望类型。
 *
 * 该工具会先处理专门的类型不匹配诊断，再把 ideal 类型按期望类型消解，并对类型变量做稳定化后
 * 交给类型系统子类型判断。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkTypeMismatch(
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    source: AbstractCjSourceElement,
    preferredSpecializedSource: AbstractCjSourceElement? = null,
    diagnosticFactory: CjDiagnosticFactory3<ConeCangJieType, ConeCangJieType, Boolean>,
) {
    if (actualType is ConeErrorType || expectedType is ConeErrorType) return
    val effectiveActualType = IdealTypeResolver.resolveIfIdeal(actualType, expectedType)
    val diagnosticSource = preferredSpecializedSource ?: source
    specificTypeMismatchDiagnostic(
        source = diagnosticSource,
        expectedType = expectedType,
        actualType = effectiveActualType,
        session = context.session,
    )?.let { diagnostic ->
        reporter.report(diagnostic, context)
        return
    }

    val normalizedActualType = effectiveActualType.normalizeForSubtypeCheck()
    val normalizedExpectedType = expectedType.normalizeForSubtypeCheck()
    if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, normalizedActualType, normalizedExpectedType) == true) return
    reporter.reportOn(
        diagnosticSource,
        diagnosticFactory,
        expectedType,
        actualType,
        false,
    )
}

/**
 * 把约束系统产生的类型变量类型规范化为原始类型参数类型。
 *
 * 这样初始化器检查能复用普通类型参数的子类型规则，而不是把临时类型变量构造器当作不同类型。
 */
private fun ConeCangJieType.normalizeForSubtypeCheck(): ConeCangJieType {
    return when (this) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
            if (originalTypeParameter != null) {
                ConeTypeParameterTypeImpl(originalTypeParameter, attributes)
            } else {
                this
            }
        }

        else -> this
    }
}
