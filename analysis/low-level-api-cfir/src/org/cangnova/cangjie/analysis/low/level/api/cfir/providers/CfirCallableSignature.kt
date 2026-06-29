/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.render.ConeAttributeRenderer
import org.cangnova.cangjie.cfir.render.ConeFullyQualifiedIdRenderer
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeAttribute
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * **Note**: the signature doesn't contain a name. This check should be done externally.
 */
@CaImplementationDetail
class CfirCallableSignature private constructor(
    /**
     * 函数值参数类型的渲染结果；非函数 callable 使用 `null`。
     */
    private val parameters: List<String>?,

    /**
     * callable 声明的类型参数数量。
     */
    private val typeParametersCount: Int,

    /**
     * callable 返回类型的稳定渲染结果。
     */
    private val returnType: String,
) {
    /**
     * 判断 [declaration] 指向的 callable 是否与当前签名一致。
     */
    fun hasTheSameSignature(declaration: CfirCallableSymbol<*>): Boolean = hasTheSameSignature(declaration.cfir)

    /**
     * 判断 [declaration] 是否与当前签名一致。
     *
     * 比较范围包含类型参数数量、函数参数类型列表和返回类型；名称比较应由调用方在外部完成。
     */
    fun hasTheSameSignature(declaration: CfirCallableDeclaration): Boolean {
        if (typeParametersCount != declaration.typeParameters.size) return false
        if (parameters?.size != (declaration as? CfirFunction)?.valueParameters?.size) return false

        declaration.lazyResolveToPhase(CfirResolvePhase.TYPES)

        if (declaration is CfirFunction) {
            requireNotNull(parameters)
            for ((index, parameter) in declaration.valueParameters.withIndex()) {
                if (parameters[index] != parameter.returnTypeRef.renderType()) return false
            }
        }

        return returnType == declaration.symbol.resolvedReturnTypeRef.renderType()
    }

    /**
     * 按参数类型、类型参数数量和返回类型比较签名对象。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CfirCallableSignature) return false

        if (parameters != other.parameters) return false
        if (typeParametersCount != other.typeParametersCount) return false
        return returnType == other.returnType

    }

    /**
     * 基于签名三元组生成哈希值。
     */
    override fun hashCode(): Int {
        var result = parameters.hashCode()
        result = 31 * result + typeParametersCount.hashCode()
        result = 31 * result + returnType.hashCode()
        return result
    }

    companion object {
        /**
         * 根据 [callableSymbol] 当前 CFIR 声明创建签名。
         */
        fun createSignature(callableSymbol: CfirCallableSymbol<*>): CfirCallableSignature = createSignature(callableSymbol.cfir)

        /**
         * 将 [callableDeclaration] 推进到类型阶段并创建可比较签名。
         */
        fun createSignature(callableDeclaration: CfirCallableDeclaration): CfirCallableSignature {
            callableDeclaration.lazyResolveToPhase(CfirResolvePhase.TYPES)

            return CfirCallableSignature(
                parameters = if (callableDeclaration is CfirFunction) {
                    callableDeclaration.valueParameters.map { it.returnTypeRef.renderType() }
                } else {
                    null
                },
                typeParametersCount = callableDeclaration.typeParameters.size,
                returnType = callableDeclaration.symbol.resolvedReturnTypeRef.renderType(),
            )
        }
    }
}

/**
 * 用最小属性渲染策略把类型引用转换为签名比较用字符串。
 */
private fun CfirTypeRef.renderType(builder: StringBuilder = StringBuilder()): String {
    val typeRenderer = ConeTypeRenderer(attributeRenderer = MinimalConeTypeAttributeRenderer).apply {
        idRenderer = ConeFullyQualifiedIdRenderer()
    }
    return CfirRenderer(
        builder = builder,
        annotationRenderer = null,
        declarationRenderer = null,
        packageDirectiveRenderer = null,
        resolvePhaseRenderer = null,
        errorExpressionRenderer = null,
        typeRenderer = typeRenderer,
        callableSignatureRenderer = null,
        modifierRenderer = null,
        inlineExpressionRenderer = null,
        patternRenderer = null,
    ).renderElementAsString(this)
}

/**
 * 签名比较使用的最小 Cone 类型属性渲染器。
 *
 * 当前没有属性会影响 low-level provider 的 callable 签名匹配，因此所有属性都被过滤。
 */
private object MinimalConeTypeAttributeRenderer : ConeAttributeRenderer() {
    /**
     * 只渲染被标记为重要的类型属性。
     */
    override fun render(attributes: Iterable<ConeAttribute<*>>): String =
        attributes.filter { it.isImportant }.let(ToString::render)

    /**
     * 当前签名比较没有需要保留的 Cone 类型属性。
     */
    private val ConeAttribute<*>.isImportant get() = false
}
