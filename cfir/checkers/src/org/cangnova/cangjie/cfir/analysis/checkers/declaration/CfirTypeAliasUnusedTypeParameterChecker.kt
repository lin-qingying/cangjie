package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.forEachType

/**
 * 检查 type alias 声明中未被展开类型使用的类型参数。
 *
 * 对齐官方 Cangjie `TypeCheckerImpl::GetUnusedTysInTypeAlias`：
 * 以别名 RHS 类型树中的类型实参递归为准，按声明顺序报告未出现的类型参数。
 */
object CfirTypeAliasUnusedTypeParameterChecker : CfirTypeAliasChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeAlias) {
        if (declaration.typeParameters.isEmpty()) return

        val expandedType = (declaration.expandedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (expandedType.contains { it is ConeErrorType }) return

        val usedTypeParameterSymbols = linkedSetOf<org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol>()
        expandedType.forEachType { type ->
            val typeParameterType = type as? ConeTypeParameterType ?: return@forEachType
            usedTypeParameterSymbols += typeParameterType.lookupTag.typeParameterSymbol
        }

        val unusedTypeParameters = declaration.typeParameters.filter { it.symbol !in usedTypeParameterSymbols }
        if (unusedTypeParameters.isEmpty()) return

        reporter.reportOn(
            source = declaration.typeAliasDeclarationHeaderDiagnosticSource(),
            factory = CfirErrors.TYPEALIAS_UNUSED_TYPE_PARAMETERS,
            a = unusedTypeParameters.joinToString(",") { "Generics-${it.name.asString()}" },
        )
    }
}
