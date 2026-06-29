/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.cfir.generator

import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.createType

/**
 * 诊断参数从 CFIR 内部类型转换到 Analysis API 公开类型的抽象规则。
 */
sealed class HLParameterConversion {
    /**
     * 生成从原始表达式得到公开参数值的源码片段。
     */
    abstract fun convertExpression(expression: String, context: ConversionContext): String

    /**
     * 将原始 CFIR 参数类型转换为公开 API 参数类型。
     */
    abstract fun convertType(type: KType): KType

    /**
     * 转换表达式需要额外导入的类型或函数。
     */
    open val importsToAdd: List<String> get() = emptyList()
}

/**
 * 不需要转换的参数规则。
 */
object HLIdParameterConversion : HLParameterConversion() {
    /**
     * 原样返回原始参数表达式。
     */
    override fun convertExpression(expression: String, context: ConversionContext) = expression

    /**
     * 原样保留原始参数类型。
     */
    override fun convertType(type: KType): KType = type
}

/**
 * 集合参数的逐元素转换规则。
 */
class HLCollectionParameterConversion(
    /**
     * 生成 map lambda 时使用的元素参数名。
     */
    private val parameterName: String,
    /**
     * 集合元素使用的转换规则。
     */
    private val mappingConversion: HLParameterConversion,
) : HLParameterConversion() {
    /**
     * 生成 `map` 调用，并在 lambda 内套用元素转换规则。
     */
    override fun convertExpression(expression: String, context: ConversionContext): String {
        val innerExpression = mappingConversion.convertExpression(parameterName, context.increaseIndent())
        return buildString {
            appendLine("$expression.map { $parameterName ->")
            appendLine(innerExpression.withIndent(context.increaseIndent()))
            append("}".withIndent(context))
        }
    }

    /**
     * 将集合参数类型转换为元素已转换后的 `List<T>` 类型。
     */
    override fun convertType(type: KType): KType =
        List::class.createType(
            arguments = listOf(
                KTypeProjection(
                    variance = KVariance.INVARIANT,
                    type = type.arguments.single().type?.let(mappingConversion::convertType)
                )
            )
        )

    /**
     * 集合转换继承元素转换所需导入。
     */
    override val importsToAdd get() = mappingConversion.importsToAdd
}

/**
 * 通过函数调用完成参数转换的规则。
 */
class HLFunctionCallConversion(
    /**
     * 包含 `{0}` 占位符的调用模板，生成时会替换为原始参数表达式。
     */
    private val callTemplate: String,
    /**
     * 函数调用返回的公开参数类型。
     */
    private val callType: KType,
    /**
     * 函数调用需要额外导入的类型或函数。
     */
    override val importsToAdd: List<String> = emptyList()
) : HLParameterConversion() {
    /**
     * 用原始表达式替换调用模板占位符。
     */
    override fun convertExpression(expression: String, context: ConversionContext) =
        callTemplate.replace("{0}", expression)

    /**
     * 返回调用模板声明的公开类型。
     */
    override fun convertType(type: KType): KType = callType
}

/**
 * 转换表达式生成时的缩进上下文。
 */
data class ConversionContext(
    /**
     * 当前缩进层级。
     */
    val currentIndent: Int,
    /**
     * 单个缩进层级占用的空格数。
     */
    val indentUnitValue: Int,
) {
    /**
     * 返回进入下一层代码块后的缩进上下文。
     */
    fun increaseIndent() = copy(currentIndent = currentIndent + 1)
}

/**
 * 按转换上下文重排多行表达式缩进。
 */
private fun String.withIndent(context: ConversionContext): String {
    val newIndent = " ".repeat(context.currentIndent * context.indentUnitValue)
    return replaceIndent(newIndent)
}
