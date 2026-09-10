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

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.match.constantPatternLiteral
import org.cangnova.cangjie.cfir.resolve.match.constantPatternLiteralExpectedType
import org.cangnova.cangjie.cfir.resolve.match.resolveLiteralPatternType
import org.cangnova.cangjie.cfir.resolve.match.CfirTuplePatternShape
import org.cangnova.cangjie.cfir.resolve.match.resolveTupleShape
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name

/**
 * 标准库 `Option.Some` 构造器名称。
 */
private val OPTION_SOME_CONSTRUCTOR_NAME = Name.identifier("Some")
/**
 * 标准库 `Option.None` 构造器名称。
 */
private val OPTION_NONE_CONSTRUCTOR_NAME = Name.identifier("None")

/**
 * 按 selector/initializer 的结构投影目标类型，再首次解析模式的值表达式。
 *
 * 数值一元运算必须从首次调用解析就获得正确目标类型，不能只在事后修改返回类型而
 * 留下另一种整数宽度的 receiver/callee。声明、Match、IfLet/WhileLet 和 for-in 共用此入口。
 * 返回已确定的值/形状检查是否成功；分量失败后只解析剩余节点，不再向它们传递目标类型。
 */
internal fun CfirPartialBodyResolveTransformer.resolvePatternValueExpressions(
    pattern: CfirPattern,
    expectedType: ConeCangJieType?,
): Boolean {
    return when (pattern) {
        is CfirConstPattern -> {
            val literalType = pattern.constantPatternLiteralExpectedType(expectedType, session)
            val mode = literalType?.let { withExpectedType(it) } ?: ResolutionMode.ContextIndependent
            pattern.transformExpression(transformer, mode)
            val literal = pattern.constantPatternLiteral()
            // 官方字符类目标只改变字符串 literal 的类型，保留 STRING kind 用于 Sema 比较。
            if (literal?.kind == CfirLiteralKind.STRING && literalType != null) {
                literal.replaceConeTypeOrNull(literalType)
            }
            constantPatternErrorType(pattern, expectedType) == null
        }
        is CfirTuplePattern -> {
            val shape = pattern.resolveTupleShape(expectedType, session)
            val tupleType = (shape as? CfirTuplePatternShape.Matched)?.tupleType
            var valid = shape !is CfirTuplePatternShape.NotTuple && shape !is CfirTuplePatternShape.WrongSize
            pattern.elements.forEachIndexed { index, element ->
                val elementType = tupleType?.elementTypes?.getOrNull(index).takeIf { valid }
                if (!resolvePatternValueExpressions(element, elementType)) valid = false
            }
            valid
        }
        is CfirEnumPattern -> {
            pattern.transformConstructorReference(transformer, ResolutionMode.ContextIndependent)
            val argumentTypes = resolveEnumArgumentTypes(pattern, expectedType)
            var valid = true
            pattern.arguments.forEachIndexed { index, argument ->
                val argumentType = argumentTypes.getOrNull(index).takeIf { valid }
                if (!resolvePatternValueExpressions(argument, argumentType)) valid = false
            }
            valid
        }
        is CfirBindingPattern -> {
            pattern.transformTypeRef(transformer, ResolutionMode.ContextIndependent)
            pattern.transformBindingVariable(transformer, ResolutionMode.ContextIndependent)
            pattern.nestedPattern?.let {
                resolvePatternValueExpressions(it, pattern.typeRef?.coneTypeOrNull ?: expectedType)
            } ?: true
        }
        is CfirOrPattern -> {
            var valid = true
            // OR 各项独立检查，不能因前一 alternative 失败而跳过后一项的目标类型。
            for (alternative in pattern.alternatives) {
                if (!resolvePatternValueExpressions(alternative, expectedType)) valid = false
            }
            valid
        }
        is CfirExpressionPattern -> {
            pattern.transformExpression(transformer, withExpectedType(session.builtinTypes.boolType))
            true
        }
        is CfirTypePattern, is CfirVarOrEnumPattern, is CfirWildcardPattern -> {
            pattern.transformChildren(transformer, ResolutionMode.ContextIndependent)
            true
        }
    }
}

/** 字面量模式失败只传播错误状态；具体诊断由常量模式/数值范围 checker 拥有。 */
internal fun CfirPartialBodyResolveTransformer.constantPatternErrorType(
    pattern: CfirConstPattern,
    expectedType: ConeCangJieType?,
): ConeErrorType? {
    val expressionError = pattern.expression.coneTypeOrNull as? ConeErrorType
    if (expressionError != null) return expressionError
    if (pattern.resolveLiteralPatternType(expectedType, session)?.failure == null) return null
    return ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("Invalid constant pattern type")))
}

/**
 * 统一的模式绑定解析支持。
 *
 * 这里负责两件事：
 * 1. 解析 pattern 自身携带的显式类型约束；
 * 2. 按 selector / initializer 的形状，把投影后的子类型写回每个 binding variable；
 * 3. 收集常量模式失败状态，阻止依赖无效模式的表达式结果类型合成。
 *
 * 这样 `let/var pattern`、`match`、`for-in` 都复用同一套绑定模型。
 */
internal fun CfirPartialBodyResolveTransformer.resolvePatternBindingTypes(
    pattern: CfirPattern,
    expectedType: ConeCangJieType?,
    typeResolver: CfirSpecificTypeResolverTransformer,
): ConeErrorType? {
    var firstError: ConeErrorType? = null
    fun recordError(error: ConeErrorType?) {
        if (firstError == null) firstError = error
    }
    when (pattern) {
        is CfirVarOrEnumPattern -> Unit
        is CfirConstPattern -> {
            // 官方 ChkConstPattern 要求字面量与 selector 类型相等，不允许 Option 装箱。
            // fresh selector 的等式必须进入共享推断系统，供 lambda 定义点完成使用。
            if (expectedType is ConeTypeVariableType) {
                val literalType = pattern.expression.coneTypeOrNull
                if (literalType != null && literalType !is ConeErrorType) {
                    context.inferenceSession.addEqualityConstraintIfCompatible(
                        expectedType,
                        IdealTypeResolver.resolveIfIdeal(literalType),
                    )
                }
            }
            recordError(constantPatternErrorType(pattern, expectedType))
        }
        is CfirBindingPattern -> {
            val resolvedTypeRef = pattern.typeRef?.let { resolvePatternTypeRefIfNeeded(it, typeResolver) }
            if (resolvedTypeRef != null && resolvedTypeRef !== pattern.typeRef) {
                pattern.transformTypeRef(typeResolver, patternTypeResolutionConfiguration())
            }

            val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
            pattern.bindingVariable?.replaceBindingType(bindingType)
            pattern.nestedPattern?.let {
                recordError(resolvePatternBindingTypes(it, bindingType ?: expectedType, typeResolver))
            }
        }

        is CfirTypePattern -> {
            val resolvedTypeRef = resolvePatternTypeRefIfNeeded(pattern.typeRef, typeResolver)
            if (resolvedTypeRef !== pattern.typeRef) {
                pattern.transformTypeRef(typeResolver, patternTypeResolutionConfiguration())
            }

            val bindingType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
            pattern.bindingVariable?.replaceBindingType(bindingType)
        }

        is CfirTuplePattern -> {
            val tupleType = expectedType?.fullyExpandedType(session) as? ConeTupleType
            pattern.elements.forEachIndexed { index, element ->
                recordError(resolvePatternBindingTypes(
                    pattern = element,
                    expectedType = tupleType?.elementTypes?.getOrNull(index),
                    typeResolver = typeResolver,
                ))
            }
        }

        is CfirEnumPattern -> {
            val argumentTypes = resolveEnumArgumentTypes(pattern, expectedType)
            pattern.arguments.forEachIndexed { index, argument ->
                recordError(resolvePatternBindingTypes(
                    pattern = argument,
                    expectedType = argumentTypes.getOrNull(index),
                    typeResolver = typeResolver,
                ))
            }
        }

        is CfirOrPattern -> pattern.alternatives.forEach { alternative ->
            recordError(resolvePatternBindingTypes(alternative, expectedType, typeResolver))
        }

        else -> Unit
    }
    return firstError
}

/**
 * 将 pattern 中所有当前作用域可见的 binding variable 注册到局部变量上下文。
 */
internal fun CfirPartialBodyResolveTransformer.registerPatternBindings(pattern: CfirPattern) {
    for (bindingVariable in pattern.visibleBindingVariables()) {
        context.storeVariable(bindingVariable, session)
    }
}

/**
 * 简单 `let/var x = initializer` 中，名字解析指向内层 binding variable，
 * 但 initializer 归属于外层 pattern variable。这里把“整个绑定值”的来源同步到
 * 内层变量，供后续数据流、可达性和诊断按普通局部变量读取。
 */
internal fun propagateWholeInitializerToSimplePatternBinding(
    pattern: CfirPattern,
    initializer: CfirExpression?,
) {
    if (initializer == null) return
    val binding = when (pattern) {
        is CfirBindingPattern if pattern.nestedPattern == null -> pattern.bindingVariable
        is CfirVarOrEnumPattern -> pattern.bindingVariable
        is CfirTypePattern -> pattern.bindingVariable
        else -> null
    } ?: return
    binding.replaceInitializer(initializer)
}

/**
 * 解析 pattern 显式类型引用；已解析或隐式类型引用直接复用。
 */
private fun CfirPartialBodyResolveTransformer.resolvePatternTypeRefIfNeeded(
    typeRef: CfirTypeRef,
    typeResolver: CfirSpecificTypeResolverTransformer,
): CfirTypeRef {
    if (typeRef is CfirResolvedTypeRef || typeRef is CfirImplicitTypeRef) return typeRef
    return typeResolver.transformTypeRef(typeRef, patternTypeResolutionConfiguration())
}

/**
 * 构造 pattern 内部类型引用解析所需的配置。
 */
private fun CfirPartialBodyResolveTransformer.patternTypeResolutionConfiguration(): CfirTypeResolutionConfiguration {
    return CfirTypeResolutionConfiguration(
        useSiteFile = context.file,
        topContainer = context.containers.lastOrNull(),
    )
}

/**
 * 根据 enum pattern 和期望类型计算每个 payload 参数的期望类型。
 */
internal fun CfirPartialBodyResolveTransformer.resolveEnumArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType?,
): List<ConeCangJieType> {
    val optionArgumentTypes = expectedType?.let { resolveStdlibOptionArgumentTypes(pattern, it) }
    if (optionArgumentTypes != null) return optionArgumentTypes

    val enumType = expectedType?.expandedPatternEnumType(session) ?: return emptyList()
    val enumDeclaration = (session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum)
        ?: return emptyList()

    val constructorAccess = pattern.constructorReference.enumPatternConstructorAccessOrNull() ?: return emptyList()
    if (!constructorAccess.matchesEnumOwner(enumDeclaration, enumType)) return emptyList()
    val enumConstructor = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .firstOrNull { candidate ->
            candidate.name == constructorAccess.constructorName && candidate.payloadArity() == pattern.arguments.size
        }
        ?: return emptyList()

    return enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
}

/**
 * 标准库 `Option<T>` 在当前类型系统中以 class-like 类型承载，
 * 但官方语义仍是 `Some(T)` / `None` 的泛型 enum。
 */
private fun resolveStdlibOptionArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType,
): List<ConeCangJieType>? {
    val constructorAccess = pattern.constructorReference.enumPatternConstructorAccessOrNull()
        ?.takeIf { it.matchesStdlibOptionOwner(expectedType) }
    val constructorName = constructorAccess?.constructorName
    val optionArgumentType = expectedType.optionElementType ?: return null
    return when {
        constructorName == OPTION_SOME_CONSTRUCTOR_NAME &&
                pattern.arguments.size == 1 &&
                optionArgumentType != null -> listOf(optionArgumentType)
        constructorName == OPTION_NONE_CONSTRUCTOR_NAME &&
                pattern.arguments.isEmpty() -> emptyList()
        constructorName == OPTION_SOME_CONSTRUCTOR_NAME ||
                constructorName == OPTION_NONE_CONSTRUCTOR_NAME -> emptyList()
        else -> null
    }
}

/**
 * 将推导出的绑定类型写回 pattern binding variable。
 */
private fun CfirPatternBindingVariable.replaceBindingType(type: ConeCangJieType?) {
    if (type == null) return
    val currentTypeRef = returnTypeRef
    if (currentTypeRef is CfirResolvedTypeRef && currentTypeRef.coneType == type) return
    replaceReturnTypeRef(currentTypeRef.resolvedTypeFromPrototype(type, currentTypeRef.source))
}
