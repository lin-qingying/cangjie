package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.substitutedPayloadParameterTypes
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker

private val OPTION_SOME_CONSTRUCTOR_NAME = Name.identifier("Some")
private val OPTION_NONE_CONSTRUCTOR_NAME = Name.identifier("None")

/**
 * `match` 的 pattern legality 独立于穷尽性。
 *
 * 这里专门处理“单个 pattern 与 subject 类型根本不相容”的情况：
 * - tuple pattern 用在非 tuple 上；
 * - enum pattern 参数个数不对；
 * - 常量 / enum / type pattern 与 subject 类型不匹配。
 */
object CfirMatchPatternLegalityChecker : CfirMatchExpressionChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return

        expression.branches.forEach { branch ->
            checkPattern(branch.pattern, subjectType)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkPattern(
        pattern: CfirPattern,
        expectedType: ConeCangJieType,
    ) {
        if (expectedType is ConeErrorType) return

        when (pattern) {
            is CfirWildcardPattern -> Unit
            is CfirVarOrEnumPattern -> Unit

            is CfirBindingPattern -> {
                val declaredType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType
                if (declaredType != null && !typesMayOverlap(declaredType, expectedType, context)) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                    return
                }
                pattern.nestedPattern?.let { nested ->
                    checkPattern(nested, declaredType ?: expectedType)
                }
            }

            is CfirTypePattern -> {
                val declaredType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: return
                if (!typesMayOverlap(declaredType, expectedType, context)) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                }
            }

            is CfirTuplePattern -> {
                val tupleType = expectedType as? ConeTupleType
                if (tupleType == null) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.TYPE_MISMATCH,
                        a = expectedType,
                        b = ConeTupleType(emptyList()),
                        c = false,
                    )
                    return
                }
                if (pattern.elements.size != tupleType.elementTypes.size) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                    return
                }
                pattern.elements.forEachIndexed { index, element ->
                    checkPattern(element, tupleType.elementTypes[index])
                }
            }

            is CfirEnumPattern -> {
                val argumentTypes = pattern.enumConstructorArgumentTypes(expectedType, context)
                if (argumentTypes == null) {
                    reporter.reportOn(
                        source = pattern.enumConstructorDiagnosticSource(),
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                    return
                }

                if (pattern.arguments.size != argumentTypes.size) {
                    reporter.reportOn(
                        source = pattern.source ?: pattern.constructorReference.source,
                        factory = CfirErrors.ENUM_PATTERN_PARAM_SIZE_ERROR,
                    )
                    return
                }

                pattern.arguments.forEachIndexed { index, argument ->
                    checkPattern(argument, argumentTypes[index])
                }
            }

            is CfirConstPattern -> {
                if (pattern.shouldReportMissingEqualityOverload(expectedType, context)) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.NOT_OVERLOAD_IN_MATCH,
                    )
                } else if (!pattern.isCompatibleWith(expectedType)) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                }
            }

            is CfirExpressionPattern -> {
                if (!pattern.isCompatibleWith(expectedType)) {
                    reporter.reportOn(
                        source = pattern.source,
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                }
            }

            is CfirOrPattern -> pattern.alternatives.forEach { alternative ->
                checkPattern(alternative, expectedType)
            }
        }
    }
}

internal fun CfirMatchExpression.hasPatternLegalityProblem(context: CheckerContext): Boolean {
    val subjectType = subject?.coneTypeOrNull ?: return false
    if (subjectType is ConeErrorType) return false
    return branches.any { branch -> branch.pattern.hasPatternLegalityProblem(subjectType, context) }
}

private fun CfirPattern.hasPatternLegalityProblem(
    expectedType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    if (expectedType is ConeErrorType) return false
    return when (this) {
        is CfirWildcardPattern -> false
        is CfirVarOrEnumPattern -> false

        is CfirBindingPattern -> {
            val declaredType = (typeRef as? CfirResolvedTypeRef)?.coneType
            val headMismatch = declaredType != null && !typesMayOverlap(declaredType, expectedType, context)
            headMismatch || (nestedPattern?.hasPatternLegalityProblem(declaredType ?: expectedType, context) == true)
        }

        is CfirTypePattern -> {
            val declaredType = (typeRef as? CfirResolvedTypeRef)?.coneType ?: return false
            !typesMayOverlap(declaredType, expectedType, context)
        }

        is CfirTuplePattern -> {
            val tupleType = expectedType as? ConeTupleType ?: return true
            elements.size != tupleType.elementTypes.size ||
                    elements.withIndex().any { (index, element) ->
                        element.hasPatternLegalityProblem(tupleType.elementTypes[index], context)
                    }
        }

        is CfirEnumPattern -> {
            val argumentTypes = enumConstructorArgumentTypes(expectedType, context) ?: return true
            arguments.size != argumentTypes.size ||
                    arguments.withIndex().any { (index, argument) ->
                        argument.hasPatternLegalityProblem(argumentTypes[index], context)
                    }
        }

        is CfirConstPattern -> !isCompatibleWith(expectedType)
        is CfirExpressionPattern -> !isCompatibleWith(expectedType)
        is CfirOrPattern -> alternatives.any { alternative -> alternative.hasPatternLegalityProblem(expectedType, context) }
    }
}

private fun typesMayOverlap(
    patternType: ConeCangJieType,
    expectedType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    return AbstractTypeChecker.isSubtypeOf(context.session.typeContext, patternType, expectedType) == true ||
            AbstractTypeChecker.isSubtypeOf(context.session.typeContext, expectedType, patternType) == true
}

private fun CfirPattern.patternText(): String =
    source?.text?.toString()?.trim().orEmpty().ifBlank { this::class.simpleName ?: "pattern" }

private fun CfirEnumPattern.enumConstructorDiagnosticSource(): AbstractCjSourceElement? {
    val source = constructorReference.source ?: source ?: return null
    return CjOffsetsOnlySourceElement(source.startOffset, source.startOffset + 1)
}

private fun ConeCangJieType.patternTypeText(): String = when (this) {
    is ConePrimitiveType -> kind.typeName
    else -> classIdOrPrimitiveClassId?.shortClassName?.asString() ?: toString()
}

private fun CfirEnumPattern.constructorName(): Name? = when (val reference = constructorReference) {
    is CfirResolvedNamedReference -> reference.name
    is CfirNamedReference -> reference.name
    else -> null
}

private fun CfirEnumPattern.enumConstructorArgumentTypes(
    expectedType: ConeCangJieType,
    context: CheckerContext,
): List<ConeCangJieType>? {
    val enumType = expectedType.expandedPatternEnumType()
    val optionArgumentTypes = resolveStdlibOptionArgumentTypes(this, enumType)
    if (optionArgumentTypes != null) return optionArgumentTypes

    if (enumType !is ConeEnumType) return null
    val enumDeclaration = context.session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum
        ?: return null
    val constructorName = constructorName() ?: return null
    val enumConstructor = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .firstOrNull { constructor -> constructor.name == constructorName }
        ?: return null
    return enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
}

private fun ConeCangJieType.expandedPatternEnumType(): ConeCangJieType = when (this) {
    is ConeTypeAliasType -> expandedType?.expandedPatternEnumType() ?: this
    else -> this
}

/**
 * 标准库 `Option<T>` 在本地类型表示中可能是 class-like，
 * 但官方语义仍是 `Some(T)` / `None` 的 enum 构造器。
 */
private fun resolveStdlibOptionArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType,
): List<ConeCangJieType>? {
    val optionType = expectedType as? ConeClassLikeType ?: return null
    if (optionType.classId != StdlibClassIds.Option) return null

    return when (pattern.constructorName()) {
        OPTION_SOME_CONSTRUCTOR_NAME -> optionType.typeArguments.singleOrNull()?.type?.let(::listOf) ?: emptyList()
        OPTION_NONE_CONSTRUCTOR_NAME -> emptyList()
        else -> null
    }
}

private fun CfirConstPattern.isCompatibleWith(expectedType: ConeCangJieType): Boolean {
    val literal = expression as? CfirLiteralExpression ?: return true
    return literal.isCompatibleWith(expectedType)
}

/**
 * 对齐官方 `sema_not_overload_in_match`：
 * selector-based match 的 const pattern 如果不是内建可比较类型，
 * 就必须能在接收者 use-site scope 中找到可用的 `==`。
 *
 * 这里刻意只对“引用型 const pattern”启用该规则：
 * - 字面量 pattern 继续沿用现有 `PATTERN_NOT_MATCH` 路径，避免回归当前基础行为；
 * - 类成员与 extend 成员统一通过 use-site scope 观察，避免退化为只看声明体。
 */
private fun CfirConstPattern.shouldReportMissingEqualityOverload(
    expectedType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    val expressionType = expression.coneTypeOrNull ?: return false
    if (expressionType is ConeErrorType) return false
    if (expectedType.hasBuiltinMatchEquality()) return false

    return !hasEqualityOverload(expectedType, expressionType, context)
}

private fun CfirExpressionPattern.isCompatibleWith(expectedType: ConeCangJieType): Boolean {
    val literal = expression as? CfirLiteralExpression ?: return true
    return literal.isCompatibleWith(expectedType)
}

private fun CfirLiteralExpression.isCompatibleWith(expectedType: ConeCangJieType): Boolean = when (kind) {
    CfirLiteralKind.BOOLEAN -> expectedType is ConePrimitiveType && expectedType.kind == PrimitiveTypeKind.BOOLEAN
    CfirLiteralKind.INT -> expectedType is ConePrimitiveType && expectedType.kind != PrimitiveTypeKind.BOOLEAN && expectedType.kind != PrimitiveTypeKind.UNIT
    CfirLiteralKind.RUNE -> expectedType is ConePrimitiveType
    CfirLiteralKind.STRING -> expectedType.classIdOrPrimitiveClassId?.shortClassName?.asString() == "String"
    CfirLiteralKind.UNIT -> expectedType is ConePrimitiveType && expectedType.kind == PrimitiveTypeKind.UNIT
    else -> true
}

private fun ConeCangJieType.hasBuiltinMatchEquality(): Boolean {
    if (this is ConePrimitiveType) return true
    return classIdOrPrimitiveClassId?.shortClassName?.asString() == "String"
}

private fun hasEqualityOverload(
    expectedType: ConeCangJieType,
    expressionType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    val classId = expectedType.classIdOrPrimitiveClassId ?: return false
    val classSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
    val scope = CfirClassUseSiteMemberScope(
        session = context.session,
        classSymbol = classSymbol,
        symbolProvider = context.session.symbolProvider,
        extendProvider = context.session.extendProvider,
        directSupertypeProvider = context.session.directSupertypeProviderOrNull,
        ownerType = expectedType,
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
    )

    var found = false
    scope.processFunctionsByName(OperatorNameConventions.EQUALS) { functionSymbol ->
        if (found || !functionSymbol.isBound) return@processFunctionsByName
        val declaration = functionSymbol.cfir as? CfirFunction ?: return@processFunctionsByName
        if (declaration.valueParameters.size != 1) return@processFunctionsByName

        val parameterType = declaration.valueParameters.single().returnTypeRef.coneTypeOrNull ?: return@processFunctionsByName
        if (!typesMayOverlap(parameterType, expressionType, context)) return@processFunctionsByName

        val returnType = declaration.returnTypeRef.coneTypeOrNull ?: return@processFunctionsByName
        if (returnType is ConePrimitiveType && returnType.kind == PrimitiveTypeKind.BOOLEAN) {
            found = true
        }
    }
    return found
}
