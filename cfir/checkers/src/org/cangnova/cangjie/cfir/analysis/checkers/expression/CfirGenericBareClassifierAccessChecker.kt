package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains

/**
 * 裸泛型 classifier 被当作值或限定符使用时，需要落到专门的 generic 诊断，
 * 而不是继续等待后续阶段退化成普通 unresolved。
 */
object CfirGenericBareClassifierAccessChecker : CfirQualifiedAccessChecker() {
    /** 检查裸泛型 classifier 访问是否缺少必须显式提供的类型实参。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.typeArguments.isNotEmpty()) return
        val resolvedSymbol = expression.calleeReference.resolvedBareAccessSymbol() ?: return

        if (resolvedSymbol is CfirEnumConstructorSymbol) {
            expression.reportEnumConstructorWithoutTypeArgumentsIfNeeded(resolvedSymbol)
            return
        }

        if (context.callsOrAssignments.asReversed().drop(1).any { call ->
                call is CfirFunctionCall && call.explicitReceiver === expression
            }) return
        if (expression.isQualifierOfEnumConstructorAccess()) return

        val resolvedReference = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val classLikeSymbol = resolvedSymbol as? CfirClassLikeSymbol<*> ?: return
        if (!classLikeSymbol.requiresExplicitTypeArgumentsForBareAccess()) return

        reporter.reportOn(
            source = resolvedReference.source ?: expression.source,
            factory = CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT,
            a = classLikeSymbol.classId.shortClassName,
        )
    }

    /**
     * enum constructor 本身不是 classifier symbol，但裸 `T1` 的 owner enum 仍可能是泛型。
     *
     * 官方在 `T1.a16(...)` 不能从 member call 推断出 owner `Test<T>` 时，同时报告
     * 函数推断失败和裸泛型 enum constructor 缺少类型实参；若 owner 已由实参或目标类型
     * 固定成 `Test<Int64>`，这里不再报裸泛型诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirQualifiedAccessExpression.reportEnumConstructorWithoutTypeArgumentsIfNeeded(
        symbol: CfirEnumConstructorSymbol,
    ) {
        val ownerSymbol = context.session.cfirProvider.getContainingClass(symbol) ?: return
        val ownerTypeParameters = ownerSymbol.cfir.typeParameters.mapTo(mutableSetOf()) { it.symbol }
        if (ownerTypeParameters.isEmpty()) return
        val constructorType = coneTypeOrNull
        if (constructorType != null && !constructorType.containsUnfixedOwnerTypeParameter(ownerTypeParameters)) return

        reporter.reportOn(
            source = calleeReference.source ?: source,
            factory = CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT,
            a = symbol.name,
        )
    }

    /**
     * typealias 的裸访问按真实展开类型判断：只有参与展开的别名参数才需要由 use-site 提供。
     * 官方 `GenerateTypeMappingForBaseExpr` 会把未参与展开的 typealias 参数从待求解映射中剔除。
     */
    private fun CfirClassLikeSymbol<*>.requiresExplicitTypeArgumentsForBareAccess(): Boolean {
        val typeParameters = cfir.typeParameters
        if (typeParameters.isEmpty()) return false
        if (this !is CfirTypeAliasSymbol) return true

        val expandedType = cfir.expandedTypeRef.coneTypeOrNull ?: return false
        return typeParameters.any { parameter ->
            expandedType.referencesTypeParameter(parameter.symbol)
        }
    }

    /** 判断类型中是否直接或间接引用了指定类型参数 symbol。 */
    private fun ConeCangJieType.referencesTypeParameter(symbol: CfirTypeParameterSymbol): Boolean =
        contains { type ->
            type is ConeTypeParameterType && type.lookupTag.typeParameterSymbol == symbol
        }

    /** 判断 enum constructor 类型是否仍含 owner 的未固定泛型参数。 */
    private fun ConeCangJieType.containsUnfixedOwnerTypeParameter(
        ownerTypeParameters: Set<CfirTypeParameterSymbol>,
    ): Boolean = contains { type ->
        when (type) {
            is ConeTypeParameterType -> type.lookupTag.typeParameterSymbol in ownerTypeParameters
            is ConeTypeVariableType -> {
                val originalTag = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                originalTag?.typeParameterSymbol in ownerTypeParameters
            }
            else -> false
        }
    }

    /** 判断当前限定访问是否只是 enum 构造器访问链中的限定符。 */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isQualifierOfEnumConstructorAccess(): Boolean {
        return context.callsOrAssignments.asReversed().drop(1).any { outer ->
            outer is CfirQualifiedAccessExpression &&
                    outer.explicitReceiver === this &&
                    outer.calleeReference.resolvesToEnumConstructor()
        }
    }

    /** 判断引用是否解析到 enum 构造器。 */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvesToEnumConstructor(): Boolean = when (this) {
        is CfirResolvedNamedReference -> resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
        is CfirResolvedAppliedCallableReference -> resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
        else -> false
    }

    /** 统一读取裸访问解析后的 symbol，兼容完成前后的 candidate/error reference。 */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvedBareAccessSymbol(): CfirBasedSymbol<*>? =
        when (this) {
            is CfirResolvedNamedReference -> resolvedSymbol
            is CfirResolvedErrorReference -> resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> candidateSymbol
            else -> null
        }
}
