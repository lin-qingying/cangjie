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
import org.cangnova.cangjie.cfir.analysis.checkers.context.accessContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.match.isMatchSubtypeOf
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.text

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
    internal fun checkPattern(
        pattern: CfirPattern,
        expectedType: ConeCangJieType,
    ) {
        if (expectedType is ConeErrorType) return

        when (pattern) {
            is CfirWildcardPattern -> Unit
            is CfirVarOrEnumPattern -> Unit

            is CfirBindingPattern -> {
                val declaredType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType
                pattern.nestedPattern?.let { nested ->
                    checkPattern(nested, declaredType ?: expectedType)
                }
            }

            is CfirTypePattern -> Unit

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
                val resolution = pattern.resolveEnumConstructorPattern(expectedType, context)
                if (resolution == null) {
                    reporter.reportOn(
                        source = pattern.enumConstructorDiagnosticSource(),
                        factory = CfirErrors.PATTERN_NOT_MATCH,
                        a = pattern.patternText(),
                    )
                    return
                }

                if (resolution is EnumPatternConstructorResolution.ArityMismatch) {
                    reporter.reportOn(
                        source = pattern.source ?: pattern.constructorReference.source,
                        factory = CfirErrors.ENUM_PATTERN_PARAM_SIZE_ERROR,
                    )
                    return
                }

                val argumentTypes = (resolution as EnumPatternConstructorResolution.Resolved).argumentTypes
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
    return branches.any { branch ->
        branch.pattern.hasDuplicatePatternBindings() ||
                branch.pattern.hasPatternLegalityProblem(subjectType, context)
    }
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
            nestedPattern?.hasPatternLegalityProblem(declaredType ?: expectedType, context) == true
        }

        is CfirTypePattern -> false

        is CfirTuplePattern -> {
            val tupleType = expectedType as? ConeTupleType ?: return true
            elements.size != tupleType.elementTypes.size ||
                    elements.withIndex().any { (index, element) ->
                        element.hasPatternLegalityProblem(tupleType.elementTypes[index], context)
                    }
        }

        is CfirEnumPattern -> {
            when (val resolution = resolveEnumConstructorPattern(expectedType, context)) {
                null,
                EnumPatternConstructorResolution.ArityMismatch,
                -> true

                is EnumPatternConstructorResolution.Resolved ->
                    arguments.withIndex().any { (index, argument) ->
                        argument.hasPatternLegalityProblem(resolution.argumentTypes[index], context)
                    }
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
 * pattern 内部重复绑定已经由声明冲突检查器报告；这里仅阻止 match 覆盖算法继续
 * 把这个无效 pattern 当成可覆盖后续分支的有效行。
 */
private fun CfirPattern.hasDuplicatePatternBindings(): Boolean {
    val seen = hashSetOf<String>()
    var duplicate = false

    fun record(name: Name?) {
        val text = name?.asString() ?: return
        if (text == "_") return
        if (!seen.add(text)) duplicate = true
    }

    fun visit(pattern: CfirPattern) {
        when (pattern) {
            is CfirBindingPattern -> {
                record(pattern.name)
                pattern.nestedPattern?.let(::visit)
            }

            is CfirTypePattern -> record(pattern.bindingName)
            is CfirTuplePattern -> pattern.elements.forEach(::visit)
            is CfirEnumPattern -> pattern.arguments.forEach(::visit)
            is CfirOrPattern -> pattern.alternatives.forEach(::visit)
            is CfirWildcardPattern,
            is CfirVarOrEnumPattern,
            is CfirConstPattern,
            is CfirExpressionPattern,
            -> Unit
        }
    }

    visit(this)
    return duplicate
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
 * 官方 `ChkTypePattern` 不会仅因静态子类型关系缺失就报 `PATTERN_NOT_MATCH`：
 * 对 class-like、泛型和带泛型分量的类型会保留运行期类型检查机会。这里把同一
 * 条规则用于 binding/type pattern 的合法性过滤，避免在 usefulness 之前过早判死。
 */
private fun typesMayOverlap(
    patternType: ConeCangJieType,
    expectedType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    val accessContext = context.accessContext(CfirAccessKind.EXTEND)
    if (patternType.isMatchSubtypeOf(expectedType, context.session, accessContext)) return true
    if (expectedType.isMatchSubtypeOf(patternType, context.session, accessContext)) return true
    return expectedType.needsRuntimeTypeCheckAgainst(patternType, context)
}

/**
 * 对齐官方 `IsNeedRuntimeCheck`。
 *
 * 双方都是 final 且至少一方有声明身份时，只有同一声明才可能运行期匹配；
 * 其它 class-like 或泛型形态都不能在 legality 阶段静态判为不匹配。
 */
private fun ConeCangJieType.needsRuntimeTypeCheckAgainst(
    targetType: ConeCangJieType,
    context: CheckerContext,
): Boolean {
    if (isFinalForRuntimeTypeCheck(context) && targetType.isFinalForRuntimeTypeCheck(context)) {
        val sourceDeclaration = runtimeDeclarationClassIdOrNull()
        val targetDeclaration = targetType.runtimeDeclarationClassIdOrNull()
        if (sourceDeclaration != null || targetDeclaration != null) {
            return sourceDeclaration == targetDeclaration
        }
    }

    return (isRuntimeClassLike() && targetType.isRuntimeClassLike()) ||
            hasRuntimeGenericShape() ||
            targetType.hasRuntimeGenericShape()
}

/**
 * 官方 runtime type check 中视作 final 的类型集合。
 */
private fun ConeCangJieType.isFinalForRuntimeTypeCheck(context: CheckerContext): Boolean {
    return when (this) {
        is ConePrimitiveType,
        is ConeStructType,
        is ConeEnumType,
        is ConeVArrayType,
        -> true

        is ConeTypeAliasType -> expandedType?.isFinalForRuntimeTypeCheck(context) == true
        is ConeClassLikeType -> {
            if (classId == StdlibClassIds.Array) return true
            val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return false
            if (!symbol.isBound) return false
            symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
            val declaration = symbol.cfir as? CfirMemberDeclaration ?: return false
            !declaration.status.isAbstract && !declaration.status.isOpen
        }

        else -> false
    }
}

/**
 * 取得运行期声明身份；primitive/tuple/function 等无声明身份类型返回 null。
 */
private fun ConeCangJieType.runtimeDeclarationClassIdOrNull(): ClassId? = when (this) {
    is ConeClassLikeType -> classId
    is ConeStructType -> classId
    is ConeEnumType -> classId
    is ConeTypeAliasType -> expandedType?.runtimeDeclarationClassIdOrNull()
    else -> null
}

/**
 * 官方 `IsClassLike` 在 pattern runtime check 中覆盖具名分类器形态。
 */
private fun ConeCangJieType.isRuntimeClassLike(): Boolean =
    this is ConeClassifierType || (this as? ConeTypeAliasType)?.expandedType?.isRuntimeClassLike() == true

/**
 * 判断类型本身或其结构分量中是否保留泛型运行期可能性。
 */
private fun ConeCangJieType.hasRuntimeGenericShape(): Boolean = when (this) {
    is ConeTypeParameterType,
    is ConeTypeVariableType,
    is ConeStubType,
    -> true

    is ConeTupleType -> elementTypes.any { it.hasRuntimeGenericShape() }
    is ConeFunctionType -> parameterTypes.any { it.hasRuntimeGenericShape() } || returnType.hasRuntimeGenericShape()
    is ConeTypeAliasType -> expandedType?.hasRuntimeGenericShape() == true ||
            typeArguments.any { it.type.hasRuntimeGenericShape() }

    else -> typeArguments.any { it.type.hasRuntimeGenericShape() }
}

/**
 * 取得 pattern 的源码展示文本。
 *
 * 源码缺失时使用 pattern 类名作为稳定占位。
 */
private fun CfirPattern.patternText(): String =
    source?.text?.toString()?.trim().orEmpty().ifBlank { this::class.simpleName ?: "pattern" }

/**
 * 取得 enum pattern 构造器诊断使用的完整 pattern 范围。
 */
private fun CfirEnumPattern.enumConstructorDiagnosticSource(): AbstractCjSourceElement? =
    source ?: constructorReference.source

/**
 * 取得类型在 pattern 诊断中的简短展示文本。
 */
private fun ConeCangJieType.patternTypeText(): String = when (this) {
    is ConePrimitiveType -> kind.typeName
    else -> classIdOrPrimitiveClassId?.shortClassName?.asString() ?: toString()
}

/**
 * enum pattern 构造器解析结果。
 *
 * 参数个数不匹配必须与“subject enum 中不存在该构造器”分开，前者报告专门的
 * `ENUM_PATTERN_PARAM_SIZE_ERROR`，后者才属于 `PATTERN_NOT_MATCH`。
 */
private sealed interface EnumPatternConstructorResolution {
    data class Resolved(val argumentTypes: List<ConeCangJieType>) : EnumPatternConstructorResolution

    data object ArityMismatch : EnumPatternConstructorResolution
}

/**
 * 按 expected enum owner、构造器名称和 pattern payload 个数解析构造器。
 *
 * 标准库 Option 先走专门语义，普通 enum 再通过 subject 类型展开和 enum 声明查找构造器。
 * 同名构造器允许具有不同 payload 形状，因此不能只按名称取第一个声明。
 */
private fun CfirEnumPattern.resolveEnumConstructorPattern(
    expectedType: ConeCangJieType,
    context: CheckerContext,
): EnumPatternConstructorResolution? {
    val optionArgumentTypes = resolveStdlibOptionArgumentTypes(this, expectedType)
    if (optionArgumentTypes != null) {
        return if (arguments.size == optionArgumentTypes.size) {
            EnumPatternConstructorResolution.Resolved(optionArgumentTypes)
        } else {
            EnumPatternConstructorResolution.ArityMismatch
        }
    }

    val enumType = expectedType.expandedPatternEnumType(context.session) ?: return null
    val enumDeclaration = context.session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum
        ?: return null
    val constructorAccess = constructorReference.enumPatternConstructorAccessOrNull() ?: return null
    if (!constructorAccess.matchesEnumOwner(enumDeclaration, enumType)) return null
    val constructorName = constructorAccess.constructorName
    val constructorsByName = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .filter { constructor -> constructor.name == constructorName }
    if (constructorsByName.isEmpty()) return null

    val enumConstructor = constructorsByName.firstOrNull { constructor ->
        constructor.payloadArity() == arguments.size
    } ?: return EnumPatternConstructorResolution.ArityMismatch

    return EnumPatternConstructorResolution.Resolved(
        enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType),
    )
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

    val constructorAccess = pattern.constructorReference.enumPatternConstructorAccessOrNull()
        ?.takeIf { it.matchesStdlibOptionOwner(expectedType) }
        ?: return null

    return when (constructorAccess.constructorName) {
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
    CfirLiteralKind.STRING ->
        expectedType.classIdOrPrimitiveClassId?.shortClassName?.asString() == "String" ||
            (isSingleCharacterStringLiteral() && expectedType.isCharacterLikePatternType())
    CfirLiteralKind.UNIT -> expectedType is ConePrimitiveType && expectedType.kind == PrimitiveTypeKind.UNIT
    else -> true
}

/**
 * 判断字符串字面量是否恰好只含一个字符。
 *
 * 仓颉中 `'A'`、`"A"`、`'''A'''`、`"""A"""` 都是字符串字面量；只有**单字符**形式才能作为
 * 字符类类型的 const pattern。
 */
private fun CfirLiteralExpression.isSingleCharacterStringLiteral(): Boolean {
    val text = value as? String ?: return false
    return text.isNotEmpty() && text.codePointCount(0, text.length) == 1
}

/**
 * 判断类型是否可由单字符字符串字面量匹配。
 *
 * 官方 `cjc` 1.0.5 探针：`match (r: Rune) { case 'A' => ... }` 与
 * `match (b: UInt8) { case 'A' => ... }` 均无诊断；同形态的 `Int64` 报
 * `sema_mismatched_types`，多字符字面量 `'AB'` 对 `Rune` 同样报错。
 * 因此仅 `Rune` 与 `UInt8`（字节）属于该集合，不可放宽到其他整数类型。
 */
private fun ConeCangJieType.isCharacterLikePatternType(): Boolean =
    this is ConePrimitiveType &&
        (kind == PrimitiveTypeKind.RUNE || kind == PrimitiveTypeKind.UINT8)

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
