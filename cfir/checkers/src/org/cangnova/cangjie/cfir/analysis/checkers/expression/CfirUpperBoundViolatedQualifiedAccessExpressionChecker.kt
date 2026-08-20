package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.checkUpperBoundViolated
import org.cangnova.cangjie.cfir.analysis.checkers.checkUpperBoundViolatedForTypealiasExpansion
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.checkers.createGenericUseSiteSubstitutor
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutionMap
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.session.symbolProvider
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
    /** 检查限定访问显式类型实参是否满足 callable 类型参数上界。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val typeArgumentRefs = expression.typeArguments
        if (typeArgumentRefs.isEmpty()) return

        val callableSymbol = expression.resolvedCallableSymbolOrNull() ?: return
        val typeParameters = callableSymbol.useSiteTypeParameters()
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

    /** 从限定访问 calleeReference 中提取已解析 callable symbol。 */
    private fun CfirQualifiedAccessExpression.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
        when (val reference = calleeReference) {
            is CfirResolvedErrorReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
            else -> null
        }

    /**
     * 构造器调用的显式类型实参属于 owner class，而不是 constructor 自身。
     * 即使候选已经是 error reference，也必须恢复 owner 的 nominal 参数以报告
     * use-site upper-bound 违例并保留后续成员解析所需的类型。
     */
    context(context: CheckerContext)
    private fun CfirCallableSymbol<*>.useSiteTypeParameters(): List<org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef> {
        val declaration = cfir
        if (declaration !is CfirConstructor) return declaration.typeParameters
        val ownerClassId = callableId.classId ?: return declaration.typeParameters
        val owner = context.session.symbolProvider
            .getClassLikeSymbolByClassId(ownerClassId)
            ?.cfir as? CfirTypeParameterRefsOwner
            ?: return declaration.typeParameters
        return owner.typeParameters + declaration.typeParameters
    }

    /** 根据显式或 dispatch receiver 类型构造 owner use-site 类型参数替换。 */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.receiverTypeParameterSubstitutions(
        callableSymbol: CfirCallableSymbol<*>,
    ): Map<TypeConstructorMarker, ConeCangJieType> {
        val receiverType = (explicitReceiver ?: dispatchReceiver)
            ?.coneTypeOrNull
            ?.fullyExpandedType(context.session)
        return createCallableOwnerUseSiteSubstitutionMap(
            session = context.session,
            callableSymbol = callableSymbol,
            receiverType = receiverType,
        )
    }
}
