package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.checkUpperBoundViolated
import org.cangnova.cangjie.cfir.analysis.checkers.checkUpperBoundViolatedForTypealiasExpansion
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.createGenericUseSiteSubstitutor
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutionMap
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 检查调用/限定访问表达式上的显式泛型实参是否满足 callable 声明侧上界。
 *
 * 对齐 Kotlin FIR `FirUpperBoundViolatedQualifiedAccessExpressionChecker`。
 * 仓颉成员函数的上界可以引用外层类型参数，因此这里额外从 receiver 类型构造
 * use-site 替换，对齐官方 C++ `GenerateTypeMappingForBaseExpr` 的语义。
 */
object CfirUpperBoundViolatedQualifiedAccessExpressionChecker : CfirQualifiedAccessChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val typeArgumentRefs = expression.typeArguments
        if (typeArgumentRefs.isEmpty()) return

        val callableSymbol = expression.resolvedCallableSymbolOrNull() ?: return
        val typeParameters = callableSymbol.cfir.typeParameters
        if (typeParameters.isEmpty() || typeArgumentRefs.size != typeParameters.size) return

        val typeArguments = typeArgumentRefs.map { it.coneTypeOrNull ?: return }
        val typeAliasConstructorInfo = (callableSymbol as? CfirConstructorSymbol)?.typeAliasConstructorInfo
        if (typeAliasConstructorInfo != null) {
            val typeAlias = typeAliasConstructorInfo.typeAliasSymbol.cfir
            checkUpperBoundViolatedForTypealiasExpansion(
                notExpandedType = ConeTypeAliasType(
                    classId = typeAliasConstructorInfo.typeAliasSymbol.classId,
                    expandedType = typeAlias.expandedTypeRef.coneTypeOrNull,
                    typeArguments = typeArguments,
                ),
                fallbackSource = expression.source,
            )
            return
        }

        val substitutor = createGenericUseSiteSubstitutor(
            typeParameters = typeParameters,
            resolvedArguments = typeArguments,
            typeContext = context.session.typeContext,
            additionalSubstitutions = expression.receiverTypeParameterSubstitutions(callableSymbol),
        )

        checkUpperBoundViolated(
            typeParameters = typeParameters,
            typeArgumentRefs = typeArgumentRefs,
            substitutor = substitutor,
            fallbackSource = expression.source,
        )
    }

    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }

    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.receiverTypeParameterSubstitutions(
        callableSymbol: CfirCallableSymbol<*>,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        val receiverType = (explicitReceiver ?: dispatchReceiver)
            ?.coneTypeOrNull
            ?.fullyExpandedType(context.session)
        return createCallableOwnerUseSiteSubstitutionMap(context.session, callableSymbol, receiverType)
    }
}
