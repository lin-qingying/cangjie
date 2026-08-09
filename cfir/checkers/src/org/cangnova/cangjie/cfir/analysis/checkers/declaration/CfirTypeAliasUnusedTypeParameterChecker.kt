package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.forEachType

/**
 * 检查 type alias 声明中未被展开类型使用的类型参数。
 *
 * 对齐官方 Cangjie `TypeCheckerImpl::GetUnusedTysInTypeAlias`：
 * 以别名 RHS 类型树中的类型实参递归为准，按声明顺序报告未出现的类型参数。
 */
object CfirTypeAliasUnusedTypeParameterChecker : CfirTypeAliasChecker() {
    /**
     * 检查 typealias 声明的类型参数是否在 RHS 展开类型中被使用。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeAlias) {
        if (declaration.typeParameters.isEmpty()) return

        val expandedTypeRef = declaration.expandedTypeRef as? CfirResolvedTypeRef ?: return
        if (expandedTypeRef is CfirErrorTypeRef && expandedTypeRef.diagnostic is ConeUnresolvedTypeQualifierError) return

        val expandedType = expandedTypeRef.typeForUnusedParameterCheck() ?: return
        if (expandedType.containsErrorType()) return

        val usedTypeParameterSymbols = linkedSetOf<org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol>()
        expandedType.forEachType { type ->
            val typeParameterType = type as? ConeTypeParameterType ?: return@forEachType
            usedTypeParameterSymbols += typeParameterType.lookupTag.typeParameterSymbol
        }

        val unusedTypeParameters = declaration.typeParameters.filter { it.symbol !in usedTypeParameterSymbols }
        if (unusedTypeParameters.isEmpty()) return

        reporter.reportOn(
            source = declaration.typeAliasDeclarationHeaderDiagnosticSource()?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.TYPEALIAS_UNUSED_TYPE_PARAMETERS,
            a = unusedTypeParameters.joinToString(",") { "Generics-${it.name.asString()}" },
        )
    }

    /**
     * 官方 `GetUnusedTysInTypeAlias` 在 cycle 诊断存在时仍基于 RHS typeArgs 计算 unused。
     * CFIR 在 SUPER_TYPES 阶段会把循环别名改写为 error type ref，因此这里读取
     * error ref 保存的 delegated/partially-resolved type ref，避免丢失 RHS 类型实参。
     */
    private fun CfirResolvedTypeRef.typeForUnusedParameterCheck() =
        when (this) {
            is CfirErrorTypeRef -> {
                val preservedTypeRef = delegatedTypeRef ?: partiallyResolvedTypeRef
                (preservedTypeRef as? CfirResolvedTypeRef)?.coneType ?: coneType
            }

            else -> coneType
        }
}
