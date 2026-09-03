package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.hasInvalidDeclaredUpperBoundsInCurrentContext
import org.cangnova.cangjie.cfir.calls.resolvedQualifierTypeParameter
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.expression.hasInvalidGenericTypeArgument
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.diagnostic.ConeUnableToInferExpressionTypeError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorNamedValueSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.isTypeParameterWithInvalidDeclaredUpperBounds

/**
 * 裸泛型 classifier 被当作值或限定符使用时，需要落到专门的 generic 诊断，
 * 而不是继续等待后续阶段退化成普通 unresolved。
 */
object CfirGenericBareClassifierAccessChecker : CfirQualifiedAccessChecker() {
    /** 检查裸泛型 classifier 访问是否缺少必须显式提供的类型实参。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.hasInvalidGenericTypeArgument()) return
        if (expression.typeArguments.isNotEmpty()) return
        val resolvedSymbol = expression.calleeReference.resolvedBareAccessSymbol() ?: return

        if (resolvedSymbol is CfirEnumConstructorSymbol) {
            expression.reportEnumConstructorWithoutTypeArgumentsIfNeeded(resolvedSymbol)
            return
        }

        if (context.callsOrAssignments.asReversed().drop(1).any { call ->
                call is CfirFunctionCall && call.explicitReceiver === expression
            }) return
        if (expression.isQualifierOfEnumConstructorWithFixedOwner()) return

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
        // qualified enum access 的 owner 泛型诊断归 qualifier 自身负责，避免在 member 上重复报告。
        if (explicitReceiver != null) return
        val errorReference = calleeReference as? CfirResolvedErrorReference
        if (errorReference?.diagnostic is ConeUnableToInferExpressionTypeError) return

        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return
        // payload constructor 的 owner 类型由调用实参和 expected type 推断；无法完成时属于调用推断失败，
        // 由 completion writer 写入 `UNABLE_TO_INFER_GENERIC_FUNC`，不能降级成裸 classifier 诊断。
        if (this is CfirFunctionCall && enumConstructor.valueParameters.isNotEmpty()) return

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
            is ConeErrorType -> type.isUninferredParameter ||
                    type.delegatedType?.containsUnfixedOwnerTypeParameter(ownerTypeParameters) == true
            else -> false
        }
    }

    /**
     * 判断当前裸 classifier 是否是 owner 已完成推断的 enum constructor qualifier。
     *
     * `Option.None`、typealias `E.None` 与 `A.a.b(1)` 的 qualifier 自身仍保持裸语法，
     * 但外层 enum constructor 的最终结果类型已经承载 expected type、alias substitution
     * 或后续 member-call constraint 固定出的 owner 实参。只有该最终 owner 仍未固定时，
     * qualifier 才应继续报告缺少类型实参。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isQualifierOfEnumConstructorWithFixedOwner(): Boolean {
        return context.callsOrAssignments.asReversed().drop(1).any { outer ->
            val constructorAccess = outer as? CfirQualifiedAccessExpression ?: return@any false
            if (constructorAccess.explicitReceiver !== this) return@any false

            val constructorSymbol = constructorAccess.calleeReference.resolvedEnumConstructorSymbolOrNull()
                ?: return@any false
            val ownerSymbol = context.session.cfirProvider.getContainingClass(constructorSymbol)
                ?: return@any false
            val ownerTypeParameters = ownerSymbol.cfir.typeParameters.mapTo(mutableSetOf()) { it.symbol }
            if (ownerTypeParameters.isEmpty()) return@any true

            val completedOwnerType = constructorAccess.coneTypeOrNull ?: return@any false
            !completedOwnerType.containsUnfixedOwnerTypeParameter(ownerTypeParameters)
        }
    }

    /** 统一读取裸访问解析后的 symbol，兼容完成前后的 candidate/error reference。 */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvedBareAccessSymbol(): CfirBasedSymbol<*>? =
        when (this) {
            is CfirResolvedNamedReference -> resolvedSymbol
            is CfirResolvedErrorReference -> resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> candidateSymbol
            else -> null
        }

    /** 统一读取完成前后 enum constructor symbol。 */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvedEnumConstructorSymbolOrNull(): CfirEnumConstructorSymbol? =
        when (this) {
            is CfirResolvedNamedReference -> resolvedSymbol as? CfirEnumConstructorSymbol
            is CfirResolvedErrorReference -> resolvedSymbol as? CfirEnumConstructorSymbol
            is CfirNamedReferenceWithCandidateBase -> candidateSymbol as? CfirEnumConstructorSymbol
            is CfirResolvedAppliedCallableReference -> resolvedSymbol as? CfirEnumConstructorSymbol
            else -> null
        }
}

/**
 * 类型参数 receiver 的非法 static 成员暴露访问检查器。
 *
 * 官方 `GetMemberAccessExposedTarget` 在类型参数上界非法且找不到 static 成员时报告
 * `sema_invalid_field_expose_access`，LLT 期望沿用现有 CFIR 诊断
 * `STATIC_VARIABLE_USE_GENERIC_PARAMETER`。
 */
object CfirInvalidFieldExposeAccessChecker : CfirQualifiedAccessChecker() {
    /** 检查 `T.x` 中非法上界类型参数 receiver 的失败 static 成员访问。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        val receiver = expression.explicitReceiver ?: return
        val typeParameter = receiver.resolvedQualifierTypeParameter() ?: return
        val receiverType = receiver.coneTypeOrNull as? ConeTypeParameterType
        val hasInvalidUpperBounds = typeParameter.cfir.hasInvalidDeclaredUpperBoundsInCurrentContext() ||
                receiverType?.isTypeParameterWithInvalidDeclaredUpperBounds(context.session) == true
        if (!hasInvalidUpperBounds) return
        if (expression.calleeReference.isResolvedMemberReference()) return

        val diagnosticSource = receiver.source ?: expression.source
        reporter.reportOn(
            source = diagnosticSource,
            factory = CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER,
            a = typeParameter.name,
        )
        diagnosticSource?.let(context::recordStaticGenericDependency)
    }

    /**
     * 已解析引用（包括携带诊断但保留候选的 resolved-error）交给现有候选诊断映射处理；
     * 当前 checker 只补没有候选的 member-not-found 分支。
     */
    private fun CfirReference.isResolvedMemberReference(): Boolean = when (this) {
        is CfirResolvedAppliedCallableReference -> true
        is CfirResolvedNamedReference -> true
        // 无候选分支会创建 CfirErrorNamedValueSymbol 保留错误表达式形状，不能把它当成已解析成员。
        is CfirNamedReferenceWithCandidateBase -> candidateSymbol !is CfirErrorNamedValueSymbol
        is CfirErrorNamedReference -> false
        else -> false
    }
}

/**
 * 检查显式 enum owner 后的成员类型实参。
 *
 * 该规则是非破坏性的合法性检查：诊断不能替换候选或表达式类型，后续调用参数检查、
 * enum 返回类型替换和 pattern 可达性仍必须基于 qualifier 已实例化的 owner 类型执行。
 */
object CfirInvalidEnumMemberTypeArgumentsChecker : CfirQualifiedAccessChecker() {
    /** 对 `TimeUnit<Int32>.Year<Int64>` 报告专用 enum member access 诊断。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirQualifiedAccessExpression) {
        if (expression.typeArguments.isEmpty()) return
        if (!expression.calleeReference.resolvesToQualifiedEnumConstructor()) return

        val ownerAccess = expression.explicitReceiver as? CfirQualifiedAccessExpression ?: return
        reporter.reportOn(
            source = ownerAccess.calleeReference.source ?: ownerAccess.source,
            factory = CfirErrors.INVALID_TYPE_PARAM_OF_ENUM_MEMBER_ACCESS,
        )
    }

    /** 兼容候选完成前后引用形态，判断目标是否为 enum constructor。 */
    private fun org.cangnova.cangjie.cfir.references.CfirReference.resolvesToQualifiedEnumConstructor(): Boolean =
        when (this) {
            is CfirResolvedNamedReference -> resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
            is CfirResolvedAppliedCallableReference ->
                resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
            is CfirNamedReferenceWithCandidateBase ->
                candidateSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
            else -> false
        }
}
