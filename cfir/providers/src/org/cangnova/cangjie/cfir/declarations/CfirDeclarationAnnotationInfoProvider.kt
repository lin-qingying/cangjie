package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.ClassId
import java.math.BigInteger
import java.util.IdentityHashMap

/** 在注解参数绑定后发布 declaration 语义；checker 和 Analysis 只消费该快照。 */
fun CfirDeclaration.publishAnnotationInfo() {
    val calls = annotations.filterIsInstance<CfirAnnotationCall>()
    val declarationTarget = annotationTargetFor()
    calls.forEach { it.replaceAnnotationTarget(declarationTarget) }
    fun has(kind: BuiltInAnnotationKind): Boolean = calls.any { it.annotationKind == kind }
    val annotation = calls.firstOrNull { it.annotationKind == BuiltInAnnotationKind.ANNOTATION }
    val targetArgument = annotation?.argumentValue("target") as? CfirArrayLiteral
    // A missing target argument means all targets; an explicitly empty array means no target.
    val targetEvaluator = AnnotationKindConstantEvaluator()
    val targetSet = when {
        annotation == null || targetArgument == null -> CangjieAnnotationTarget.entries.toSet()
        else -> targetArgument.elements.mapNotNull(targetEvaluator::evaluate).toSet()
    }
    val overflow = calls.lastOrNull { it.annotationKind == BuiltInAnnotationKind.NUMERIC_OVERFLOW }?.let { call ->
        // Overflow is parser-owned syntax.  Its optional square-bracket
        // identifier is intentionally not an ordinary named argument, so it
        // is read from the preserved raw argument surface when no mapping
        // exists.
        val requested = call.stringArgument("strategy") ?: call.rawOverflowStrategyArgument()
        if (requested == null) call.builtInDescriptor?.overflowStrategy else when (requested.lowercase()) {
            "no" -> CangjieOverflowStrategy.NA
            "checked" -> CangjieOverflowStrategy.CHECKED
            "wrapping" -> CangjieOverflowStrategy.WRAPPING
            "throwing" -> CangjieOverflowStrategy.THROWING
            "saturating" -> CangjieOverflowStrategy.SATURATING
            else -> CangjieOverflowStrategy.NA
        }
    } ?: serializedInteropFacts?.overflowStrategy
    val attributes = calls.filter { it.annotationKind == BuiltInAnnotationKind.ATTRIBUTE }.flatMap { call ->
        val original = (call.argumentList as? CfirResolvedArgumentList)?.originalArgumentList ?: call.argumentList
        original.arguments.mapNotNull { expression ->
            when (expression) {
                is CfirLiteralExpression -> expression.value as? String
                is CfirQualifiedAccessExpression -> (expression.calleeReference as? CfirNamedReference)?.name?.asString()
                else -> null
            }
        }
    } + serializedInteropFacts?.attributeNames.orEmpty()
    val serialized = annotationInfo
    annotationInfo = CfirDeclarationAnnotationInfo(
        isAnnotation = annotation != null || serialized?.isAnnotation == true,
        annotationTargets = if (annotation == null && serialized != null) serialized.annotationTargets else targetSet,
        isIntrinsic = has(BuiltInAnnotationKind.INTRINSIC) || serializedInteropFacts?.isIntrinsic == true,
        isConstSafe = has(BuiltInAnnotationKind.CONSTSAFE),
        isMockSupported = has(BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK),
        attributes = attributes.distinct(),
        overflowStrategy = overflow ?: serialized?.overflowStrategy,
        runtimeVisible = serialized?.runtimeVisible == true,
    )
}

/** 从已解析的 AnnotationKind 枚举常量提取官方十类目标。 */
/**
 * 计算 `@Annotation.target` 的 AnnotationKind 常量值。
 *
 * 官方 const-eval 不是按源码名字寻找目标：它先完成普通表达式解析，再从 enum
 * constructor、const initializer 或 const function 的返回值构造目标位。这里保留
 * 同一层次的结构化值传播，避免把尚未物化为 [CfirConstantValueExpression] 的
 * qualified access / call 错误地当成未知目标。
 *
 * 该 evaluator 只消费已经由 CFIR resolve 完成的 symbol、参数映射和声明 body，
 * 不重新解析 PSI，也不把短名或 source spelling 当作语义事实源。未知值和递归值
 * 返回 null，使 `CfirConstExpressionEvaluator` 继续负责 EXPECT_CONST 等诊断。
 */
private class AnnotationKindConstantEvaluator {
    private sealed interface Value {
        data class Target(val target: CangjieAnnotationTarget) : Value
        data class BooleanValue(val value: Boolean) : Value
        data class IntegerValue(val value: BigInteger) : Value
        data object Unknown : Value
    }

    private val activeSymbols = IdentityHashMap<CfirBasedSymbol<*>, Boolean>()
    private val environments = ArrayDeque<MutableMap<CfirBasedSymbol<*>, Value>>()

    fun evaluate(expression: CfirExpression): CangjieAnnotationTarget? =
        evaluateValue(expression).targetOrNull()

    private fun evaluateValue(expression: CfirExpression): Value = when (expression) {
        is CfirNamedArgumentExpression -> evaluateValue(expression.expression)

        is CfirConstantValueExpression -> {
            val constructor = (expression.constantValue as? CfirConstantValue.EnumValue)?.constructor
            constructor?.annotationTargetValue() ?: Value.Unknown
        }

        is CfirLiteralExpression -> when (expression.value) {
            is Boolean -> Value.BooleanValue(expression.value as Boolean)
            is BigInteger -> Value.IntegerValue(expression.value as BigInteger)
            else -> Value.Unknown
        }

        is CfirFunctionCall -> evaluateFunctionCall(expression)
        is CfirQualifiedAccessExpression -> evaluateQualifiedAccess(expression)
        is CfirReturnExpression -> evaluateValue(expression.result)
        is CfirBlock -> evaluateBlock(expression)
        is CfirIfExpression -> evaluateIf(expression)
        is CfirTypeOperator -> evaluateValue(expression.argument)
        is CfirTypeConversion -> evaluateValue(expression.argument)
        else -> Value.Unknown
    }

    private fun evaluateQualifiedAccess(expression: CfirQualifiedAccessExpression): Value {
        val symbol = (expression.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
            ?: return Value.Unknown
        if (expression.explicitReceiver != null || expression.dispatchReceiver != null) {
            // Static enum constructor access has no runtime receiver. A real receiver
            // must be evaluated before a member value can be considered constant.
            if (symbol !is CfirEnumConstructorSymbol) return Value.Unknown
        }

        if (symbol is CfirEnumConstructorSymbol) {
            symbol.annotationTargetValue().takeUnless { it === Value.Unknown }?.let { return it }
        }
        environments.asReversed().firstNotNullOfOrNull { it[symbol] }?.let { return it }

        val variable = symbol.cfir as? CfirVariable ?: return Value.Unknown
        if (!variable.status.isConst || activeSymbols.put(symbol, true) != null) return Value.Unknown
        return try {
            variable.initializer?.let(::evaluateValue) ?: Value.Unknown
        } finally {
            activeSymbols.remove(symbol)
        }
    }

    private fun evaluateFunctionCall(expression: CfirFunctionCall): Value {
        val symbol = (expression.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
            ?: return Value.Unknown
        val function = symbol.cfir as? CfirFunction ?: return Value.Unknown
        if (!function.status.isConst || activeSymbols.put(symbol, true) != null) return Value.Unknown

        val bindings = linkedMapOf<CfirBasedSymbol<*>, Value>()
        val resolvedArguments = expression.argumentList as? CfirResolvedArgumentList
        val explicitArguments = resolvedArguments?.originalArgumentList?.arguments
            ?: expression.argumentList.arguments
        val mapping = resolvedArguments?.mapping.orEmpty()
        for (argument in explicitArguments) {
            val valueExpression = (argument as? CfirNamedArgumentExpression)?.expression ?: argument
            val parameter = mapping[argument] ?: mapping[valueExpression] ?: continue
            bindings[parameter.symbol] = evaluateValue(valueExpression)
        }

        environments.addLast(bindings)
        return try {
            evaluateBlock(function.body)
        } finally {
            environments.removeLast()
            activeSymbols.remove(symbol)
        }
    }

    private fun evaluateBlock(block: CfirBlock?): Value {
        block ?: return Value.Unknown
        var lastValue: Value = Value.Unknown
        for (statement in block.statements) {
            when (statement) {
                is CfirReturnExpression -> return evaluateValue(statement.result)
                is CfirVariable -> {
                    val initializer = statement.initializer
                    if (initializer != null) {
                        val value = evaluateValue(initializer)
                        environments.lastOrNull()?.set(statement.symbol, value)
                    }
                }
                is CfirExpression -> lastValue = evaluateValue(statement)
            }
        }
        return lastValue
    }

    private fun evaluateIf(expression: CfirIfExpression): Value {
        return when (val condition = evaluateValue(expression.condition)) {
            is Value.BooleanValue -> if (condition.value) {
                evaluateBlock(expression.thenBranch)
            } else {
                evaluateValue(expression.elseBranch ?: return Value.Unknown)
            }
            else -> Value.Unknown
        }
    }

    private fun CfirEnumConstructorSymbol.annotationTargetValue(): Value {
        if (callableId.classId != ClassId.fromString("std/core/AnnotationKind")) return Value.Unknown
        return CangjieAnnotationTarget.entries
            .firstOrNull { it.sourceName == name.asString() }
            ?.let(Value::Target)
            ?: Value.Unknown
    }

    private fun Value.targetOrNull(): CangjieAnnotationTarget? =
        (this as? Value.Target)?.target
}

/** Read the optional parser-owned overflow identifier without scanning source text. */
private fun CfirAnnotationCall.rawOverflowStrategyArgument(): String? {
    val argument = argumentView?.entries
        ?.firstOrNull { !it.isDefaultOrigin }
        ?.argument
        ?: argumentList.arguments.firstOrNull()
    val expression = (argument as? CfirNamedArgumentExpression)?.expression ?: argument
    return when (expression) {
        is CfirLiteralExpression -> expression.value as? String
        is CfirQualifiedAccessExpression ->
            (expression.calleeReference as? CfirNamedReference)?.name?.asString()
        else -> null
    }
}
