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
import org.cangnova.cangjie.cfir.expressions.CfirCallExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.match.constantPatternLiteral
import org.cangnova.cangjie.cfir.resolve.match.resolveTypePatternMatchingKind
import org.cangnova.cangjie.cfir.resolve.match.constantPatternLiteralExpectedType
import org.cangnova.cangjie.cfir.resolve.match.resolveLiteralPatternType
import org.cangnova.cangjie.cfir.resolve.match.CfirTuplePatternShape
import org.cangnova.cangjie.cfir.resolve.match.resolveTupleShape
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.inference.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPostponedAtomsResolutionContext
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
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
            // 先解析构造器限定符；真正的构造器目标需要结合 selector 类型和载荷个数决定。
            val access = when (val constructor = pattern.constructor) {
                is CfirQualifiedAccessExpression -> constructor
                is CfirCallExpression -> constructor.base as? CfirQualifiedAccessExpression
                else -> null
            }
            if (access != null) {
                transformer.expressionsTransformer.transformExplicitReceiverOf(access, ResolutionMode.ReceiverResolution)
            } else {
                pattern.transformConstructor(transformer, ResolutionMode.ContextIndependent)
            }
            val argumentTypes = resolveEnumArgumentTypes(pattern, expectedType)
            var valid = true
            pattern.arguments.forEachIndexed { index, argument ->
                val argumentType = argumentTypes.componentType(index).takeIf { valid }
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
        is CfirCatchPattern -> resolvePatternValueExpressions(pattern.nestedPattern, expectedType)
        is CfirCommandTypePattern -> resolvePatternValueExpressions(pattern.nestedPattern, expectedType)
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
    registerPatternBindingCompletion(pattern, expectedType)
    var firstError: ConeErrorType? = null
    fun recordError(error: ConeErrorType?) {
        if (firstError == null) firstError = error
    }
    when (pattern) {
        is CfirVarOrEnumPattern -> pattern.bindingVariable?.replaceBindingType(expectedType)
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
            recordError(resolvePatternBindingTypes(pattern.nestedPattern, bindingType, typeResolver))
            val matching = resolveTypePatternMatchingKind(expectedType, bindingType, session)
            pattern.replaceMatchingKind(matching)
            // 原生两个位独立保存；UNKNOWN 保留未完成/错误恢复状态，不捏造某种匹配结论。
            when (matching) {
                org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind.ALWAYS -> {
                    pattern.replaceMatchBeforeRuntime(true); pattern.replaceNeedRuntimeTypeCheck(false)
                }
                org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind.RUNTIME -> {
                    pattern.replaceMatchBeforeRuntime(false); pattern.replaceNeedRuntimeTypeCheck(true)
                }
                org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind.NEVER -> {
                    pattern.replaceMatchBeforeRuntime(false); pattern.replaceNeedRuntimeTypeCheck(false)
                }
                org.cangnova.cangjie.cfir.patterns.CfirTypePatternMatchingKind.UNKNOWN -> Unit
            }
            if (bindingType != null) pattern.replaceSemanticTypeRef(bindingType.toCfirResolvedTypeRef(pattern.source))
        }

        is CfirTuplePattern -> {
            val projection = resolveTupleBindingTypes(pattern, expectedType)
            recordError(projection.errorType)
            pattern.elements.forEachIndexed { index, element ->
                recordError(resolvePatternBindingTypes(
                    pattern = element,
                    expectedType = projection.componentType(index),
                    typeResolver = typeResolver,
                ))
            }
        }

        is CfirEnumPattern -> {
            val argumentTypes = resolveEnumArgumentTypes(pattern, expectedType)
            recordError(argumentTypes.errorType)
            pattern.arguments.forEachIndexed { index, argument ->
                recordError(resolvePatternBindingTypes(
                    pattern = argument,
                    expectedType = argumentTypes.componentType(index),
                    typeResolver = typeResolver,
                ))
            }
        }

        is CfirOrPattern -> pattern.alternatives.forEach { alternative ->
            recordError(resolvePatternBindingTypes(alternative, expectedType, typeResolver))
        }

        is CfirCatchPattern -> recordError(resolvePatternBindingTypes(pattern.nestedPattern, expectedType, typeResolver))
        is CfirCommandTypePattern -> recordError(resolvePatternBindingTypes(pattern.nestedPattern, expectedType, typeResolver))
        is CfirWildcardPattern, is CfirExpressionPattern -> Unit
    }
    // 此处是正常检查的类型发布点；reader 仍只读取原 P.types[0]，不会从 selector/变量反推保存槽。
    if (pattern !is CfirTypePattern && pattern !is CfirCatchPattern &&
        pattern !is CfirCommandTypePattern && expectedType != null) {
        pattern.replaceSemanticTypeRef((firstError ?: expectedType).toCfirResolvedTypeRef(pattern.source))
    }
    // 完整投影后登记实际采用的 Option payload；tuple/binding 递归沿同一个正式入口执行。
    val adoptedElement = firstError ?: expectedType?.optionElementType?.fullyExpandedType(session)
    if (adoptedElement != null) {
        context.consumeInferenceType(pattern,
            org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceVariableRole.OPTION_PATTERN_ELEMENT, adoptedElement)
    }
    return firstError
}

/** 保存真正模式宿主及其解析上下文，让 completion 在同一棵树中执行第一次或后续投影。 */
private fun CfirPartialBodyResolveTransformer.registerPatternBindingCompletion(
    pattern: CfirPattern,
    expectedType: ConeCangJieType?,
) {
    val inferenceSession = context.inferenceSession as? CfirPCLAInferenceSession ?: return
    val type = expectedType ?: return
    if (!inferenceSession.containsRegisteredInferenceVariable(type)) return
    val owner = pattern.patternTreeOwner ?: return
    if (owner.patternTreeRoot() !== pattern) return
    val savedContext = CfirPostponedAtomsResolutionContext(context.towerDataContext, inferenceSession)
    inferenceSession.registerPatternBindingProjection(owner) { substitutor ->
        val resolvedType = substitutor.substituteOrSelf(type)
        // provisional completion 仍未固定 selector 时保留任务，最终 writer 会再次执行同一回调。
        if (!resolvedType.contains { it is ConeTypeVariableType }) {
            context.withTemporaryRegularContext(savedContext) {
                transformer.expressionsTransformer.completePatternBindingProjection(owner, resolvedType)
            }
        }
    }
}

/**
 * 模式分量投影的完整结果。Deferred 只表示推断尚未完成，不能与确定的形状失败共用空列表。
 * 失败沿原诊断传播；模式 checker 仍拥有错误形状的源码诊断。
 */
internal sealed interface CfirPatternComponentTypes {
    val errorType: ConeErrorType?

    data class Matched(val types: List<ConeCangJieType>) : CfirPatternComponentTypes {
        override val errorType: ConeErrorType? get() = null
    }

    data class Failed(override val errorType: ConeErrorType) : CfirPatternComponentTypes

    data object Deferred : CfirPatternComponentTypes {
        override val errorType: ConeErrorType? get() = null
    }

    fun componentType(index: Int): ConeCangJieType? = when (this) {
        is Matched -> types[index]
        is Failed -> errorType
        Deferred -> null
    }
}

/** 当前层的 tuple 形状必须完整匹配，不能以短列表部分配对错误元数的模式。 */
private fun CfirPartialBodyResolveTransformer.resolveTupleBindingTypes(
    pattern: CfirTuplePattern,
    expectedType: ConeCangJieType?,
): CfirPatternComponentTypes {
    val currentType = when (val inference = context.inferenceSession) {
        is CfirPCLAInferenceSession -> expectedType?.let(inference::substituteCurrentType)
        else -> expectedType
    }
    val type = currentType?.fullyExpandedType(session) ?: return CfirPatternComponentTypes.Deferred
    if (type is ConeErrorType) return CfirPatternComponentTypes.Failed(type)
    return when (val shape = pattern.resolveTupleShape(type, session)) {
        CfirTuplePatternShape.Unresolved -> CfirPatternComponentTypes.Deferred
        is CfirTuplePatternShape.Matched -> CfirPatternComponentTypes.Matched(shape.tupleType.elementTypes)
        is CfirTuplePatternShape.NotTuple, is CfirTuplePatternShape.WrongSize ->
            failedPatternProjection("Invalid tuple pattern shape")
    }
}

private fun failedPatternProjection(reason: String): CfirPatternComponentTypes.Failed =
    CfirPatternComponentTypes.Failed(
        ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic(reason))),
    )

/**
 * 将 pattern 中所有当前作用域可见的 binding variable 注册到局部变量上下文。
 */
internal fun CfirPartialBodyResolveTransformer.registerPatternBindings(pattern: CfirPattern) {
    for (bindingVariable in pattern.visibleBindingVariables()) {
        context.storeVariable(bindingVariable, session)
    }
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
): CfirPatternComponentTypes {
    val currentType = when (val inference = context.inferenceSession) {
        is CfirPCLAInferenceSession -> expectedType?.let(inference::substituteCurrentType)
        else -> expectedType
    }
    val type = currentType?.fullyExpandedType(session) ?: return CfirPatternComponentTypes.Deferred
    if (type is ConeErrorType) return CfirPatternComponentTypes.Failed(type)
    if (type is ConeTypeVariableType) return CfirPatternComponentTypes.Deferred
    val optionArgumentTypes = resolveStdlibOptionArgumentTypes(pattern, type, session)
    if (optionArgumentTypes != null) {
        return if (optionArgumentTypes.size == pattern.arguments.size) {
            CfirPatternComponentTypes.Matched(optionArgumentTypes)
        } else {
            failedPatternProjection("Invalid Option pattern payload")
        }
    }

    val enumType = type.expandedPatternEnumType(session) ?: return failedPatternProjection("Pattern target is not an enum")
    val enumDeclaration = (session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum)
        ?: return failedPatternProjection("Enum pattern declaration is unresolved")

    val constructorName = pattern.constructorNameOrNull() ?: return failedPatternProjection("Enum pattern constructor is unresolved")
    if (!pattern.ownerQualifierMatchesSubject(enumType, session)) return failedPatternProjection("Enum pattern owner does not match target")
    val enumConstructor = enumDeclaration.declarations
        .filterIsInstance<CfirEnumConstructor>()
        .firstOrNull { candidate ->
            candidate.name == constructorName && candidate.payloadArity() == pattern.arguments.size
        }
        ?: return failedPatternProjection("Enum pattern constructor or payload arity does not match")

    val parameterTypes = enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
    return if (parameterTypes.size == pattern.arguments.size) {
        CfirPatternComponentTypes.Matched(parameterTypes)
    } else {
        failedPatternProjection("Enum pattern payload types are unresolved")
    }
}

/**
 * 标准库 `Option<T>` 在当前类型系统中以 class-like 类型承载，
 * 但官方语义仍是 `Some(T)` / `None` 的泛型 enum。
 */
private fun resolveStdlibOptionArgumentTypes(
    pattern: CfirEnumPattern,
    expectedType: ConeCangJieType,
    session: org.cangnova.cangjie.cfir.session.CfirSession,
): List<ConeCangJieType>? {
    val constructorName = pattern.constructorNameOrNull()
        ?.takeIf { pattern.ownerQualifierMatchesSubject(expectedType, session) }
    /*
     * Option 的元素类型可能是跨文件 typealias（例如 `?MessageHandler`）。
     * 绑定类型必须投影其展开后的类型；否则 `Some(handler)` 得到的函数值仍是 alias 类型，
     * 后续 `handler(x)` 会因看不到函数类型而报 WRONG_NUMBER_OF_ARGUMENTS。
     */
    val optionArgumentType = expectedType.optionElementType?.fullyExpandedType(session) ?: return null
    return when {
        constructorName == OPTION_SOME_CONSTRUCTOR_NAME &&
                pattern.arguments.size == 1 -> listOf(optionArgumentType)
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
/** 选择器检查失败时只完成现有绑定类型，case 模式和正文仍保留其未检查的源码树。 */
internal fun CfirPattern.completeBindingsAfterSelectorFailure(error: ConeErrorType) {
    bindingVariables().forEach { it.replaceBindingType(error) }
}

private fun CfirPatternBindingVariable.replaceBindingType(type: ConeCangJieType?) {
    if (type == null) return
    val currentTypeRef = returnTypeRef
    if (currentTypeRef is CfirResolvedTypeRef && currentTypeRef.coneType == type) return
    replaceReturnTypeRef(currentTypeRef.resolvedTypeFromPrototype(type, currentTypeRef.source))
}
