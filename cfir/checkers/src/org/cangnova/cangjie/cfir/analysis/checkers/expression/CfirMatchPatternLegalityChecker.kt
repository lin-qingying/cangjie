/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 标准库 `Option.Some` 构造器名称。
 */
private val OPTION_SOME_CONSTRUCTOR_NAME = Name.identifier("Some")

/**
 * 标准库 `Option.None` 构造器名称。
 */
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
    /**
     * 检查 selector-based match 中每个 pattern 与 subject 类型是否兼容。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirMatchExpression) {
        val subjectType = expression.subject?.coneTypeOrNull ?: return
        if (subjectType is ConeErrorType) return

        expression.branches.forEach { branch ->
            checkPattern(branch.pattern, subjectType)
        }
    }

    /**
     * 按 pattern 形态递归检查其与期望类型的合法性。
     *
     * tuple、enum、binding、type、const、expression 与 or pattern 各自对应不同的诊断规则。
     */
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

            is CfirOrPattern -> {
                if (pattern.hasVariableBindingInOrPattern() || pattern.hasDifferentOrPatternKinds()) return
                pattern.alternatives.forEach { alternative ->
                    checkPattern(alternative, expectedType)
                }
            }
        }
    }
}

/**
 * 判断 match 表达式中是否存在会阻止穷尽性分析的 pattern 合法性问题。
 *
 * 该函数供穷尽性和不可达检查复用，避免在 pattern 已经不合法时继续运行覆盖算法。
 */
internal fun CfirMatchExpression.hasPatternLegalityProblem(context: CheckerContext): Boolean {
    val subjectType = subject?.coneTypeOrNull ?: return false
    if (subjectType is ConeErrorType) return false
    return branches.any { branch -> branch.pattern.hasPatternLegalityProblem(subjectType, context) }
}

/**
 * 递归判断单个 pattern 是否与期望类型不兼容。
 */
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
        is CfirOrPattern -> {
            hasVariableBindingInOrPattern() ||
                    hasDifferentOrPatternKinds() ||
                    alternatives.any { alternative -> alternative.hasPatternLegalityProblem(expectedType, context) }
        }
    }
}

/**
 * 判断 or pattern 中是否包含变量绑定。
 *
 * 当前语义不允许 or pattern 绑定变量，因此这种情况本身就是合法性问题。
 */
private fun CfirOrPattern.hasVariableBindingInOrPattern(): Boolean =
    bindingOccurrences().isNotEmpty()

/**
 * 判断 or pattern 的各分支是否混用了不同 pattern 类别。
 */
private fun CfirOrPattern.hasDifferentOrPatternKinds(): Boolean {
    if (alternatives.size < 2) return false
    val firstKind = alternatives.first().orPatternKindKey()
    return alternatives.drop(1).any { alternative -> alternative.orPatternKindKey() != firstKind }
}

/**
 * 取得 or pattern 分支用于同类比较的类别键。
 */
private fun CfirPattern.orPatternKindKey(): String = when (this) {
    is CfirEnumPattern,
    is CfirVarOrEnumPattern,
    -> "enum-or-variable"
    is CfirConstPattern -> "constant"
    is CfirTypePattern -> "type"
    is CfirTuplePattern -> "tuple"
    is CfirWildcardPattern -> "wildcard"
    is CfirBindingPattern -> "binding"
    is CfirExpressionPattern -> "expression"
    is CfirOrPattern -> "or"
}

/**
 * 判断 pattern 类型与 subject 期望类型是否可能有交集。
 *
 * 两个方向任一方向存在子类型关系即可视为可能匹配。
 */
private fun typesMayOverlap(
    patternType: ConeCangJieType,
    expectedType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    return AbstractTypeChecker.isSubtypeOf(context.session.typeContext, patternType, expectedType) == true ||
            AbstractTypeChecker.isSubtypeOf(context.session.typeContext, expectedType, patternType) == true
}

/**
 * 取得 pattern 的源码展示文本。
 *
 * 源码缺失时使用 pattern 类名作为稳定占位。
 */
private fun CfirPattern.patternText(): String =
    source?.text?.toString()?.trim().orEmpty().ifBlank { this::class.simpleName ?: "pattern" }

/**
 * 取得 enum pattern 构造器诊断使用的首字符范围。
 */
private fun CfirEnumPattern.enumConstructorDiagnosticSource(): AbstractCjSourceElement? {
    val source = constructorReference.source ?: source ?: return null
    return CjOffsetsOnlySourceElement(source.startOffset, source.startOffset + 1)
}

/**
 * 取得类型在 pattern 诊断中的简短展示文本。
 */
private fun ConeCangJieType.patternTypeText(): String = when (this) {
    is ConePrimitiveType -> kind.typeName
    else -> classIdOrPrimitiveClassId?.shortClassName?.asString() ?: toString()
}

/**
 * 从 enum pattern 构造器引用中提取构造器名称。
 */
private fun CfirEnumPattern.constructorName(): Name? = when (val reference = constructorReference) {
    is CfirResolvedNamedReference -> reference.name
    is CfirNamedReference -> reference.name
    else -> null
}

/**
 * 解析 enum pattern 构造器的 payload 参数类型列表。
 *
 * 标准库 Option 先走专门语义，普通 enum 再通过 subject 类型展开和 enum 声明查找构造器。
 */
private fun CfirEnumPattern.enumConstructorArgumentTypes(
    expectedType: ConeCangJieType,
    context: CheckerContext,
): List<ConeCangJieType>? {
    val optionArgumentTypes = resolveStdlibOptionArgumentTypes(this, expectedType)
    if (optionArgumentTypes != null) return optionArgumentTypes

    val enumType = expectedType.expandedPatternEnumType(context.session) ?: return null
    val enumDeclaration = context.session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum
        ?: return null
    val constructorName = constructorName() ?: return null
    val enumConstructor = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .firstOrNull { constructor -> constructor.name == constructorName }
        ?: return null
    return enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
}

/**
 * 标准库 `Option<T>` 在本地类型表示中可能是 class-like，
 * 但官方语义仍是 `Some(T)` / `None` 的 enum 构造器。
 */
private fun resolveStdlibOptionArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType,
): List<ConeCangJieType>? {
    val optionArgumentType = expectedType.optionElementType ?: return null

    return when (pattern.constructorName()) {
        OPTION_SOME_CONSTRUCTOR_NAME -> listOf(optionArgumentType)
        OPTION_NONE_CONSTRUCTOR_NAME -> emptyList()
        else -> null
    }
}

/**
 * 判断常量 pattern 是否与期望类型兼容。
 */
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

/**
 * 判断表达式 pattern 是否与期望类型兼容。
 */
private fun CfirExpressionPattern.isCompatibleWith(expectedType: ConeCangJieType): Boolean {
    val literal = expression as? CfirLiteralExpression ?: return true
    return literal.isCompatibleWith(expectedType)
}

/**
 * 判断字面量 pattern 是否可匹配期望类型。
 */
private fun CfirLiteralExpression.isCompatibleWith(expectedType: ConeCangJieType): Boolean = when (kind) {
    CfirLiteralKind.BOOLEAN -> expectedType is ConePrimitiveType && expectedType.kind == PrimitiveTypeKind.BOOLEAN
    CfirLiteralKind.INT -> expectedType is ConePrimitiveType && expectedType.kind != PrimitiveTypeKind.BOOLEAN && expectedType.kind != PrimitiveTypeKind.UNIT
    CfirLiteralKind.RUNE -> expectedType is ConePrimitiveType
    CfirLiteralKind.STRING -> expectedType.classIdOrPrimitiveClassId?.shortClassName?.asString() == "String"
    CfirLiteralKind.UNIT -> expectedType is ConePrimitiveType && expectedType.kind == PrimitiveTypeKind.UNIT
    else -> true
}

/**
 * 判断类型是否具有 match const pattern 所需的内建相等比较。
 */
private fun ConeCangJieType.hasBuiltinMatchEquality(): Boolean {
    if (this is ConePrimitiveType) return true
    return classIdOrPrimitiveClassId?.shortClassName?.asString() == "String"
}

/**
 * 在期望类型的 use-site scope 中查找可用于 match 比较的 `==` 重载。
 *
 * 只有单参数、参数类型与 const 表达式类型可能相交、返回 Boolean 的函数才算有效。
 */
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
