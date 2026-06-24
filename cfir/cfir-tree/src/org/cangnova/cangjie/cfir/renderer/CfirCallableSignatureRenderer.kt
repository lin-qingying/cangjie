package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.name.SpecialNames

/**
 * callable 签名渲染器。
 *
 * 负责渲染参数列表、参数名、类型、默认值和与参数相关的注解/修饰符。
 */
open class CfirCallableSignatureRenderer {
    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前输出 printer。
     */
    protected val printer: CfirPrinter get() = components.printer

    /**
     * 当前渲染 visitor。
     */
    protected val visitor: CfirRenderer.Visitor get() = components.visitor

    /**
     * 注解渲染器。
     */
    private val annotationRenderer: CfirAnnotationRenderer? get() = components.annotationRenderer

    /**
     * 声明渲染器。
     */
    protected val declarationRenderer: CfirDeclarationRenderer? get() = components.declarationRenderer

    /**
     * 修饰符渲染器。
     */
    private val modifierRenderer: CfirModifierRenderer? get() = components.modifierRenderer

    /**
     * cone 类型渲染器。
     */
    protected val typeRenderer: ConeTypeRenderer get() = components.typeRenderer

    /**
     * 渲染 value parameter 列表。
     */
    fun renderParameters(valueParameters: List<CfirValueParameter>) {
        printer.print("(")
        for ((index, valueParameter) in valueParameters.withIndex()) {
            if (index > 0) {
                printer.print(", ")
            }
            renderParameter(valueParameter)
        }
        printer.print(")")
    }

    /**
     * 渲染单个 value parameter。
     */
    fun renderParameter(valueParameter: CfirValueParameter) {
        declarationRenderer?.renderPhaseAndAttributes(valueParameter)
        annotationRenderer?.render(valueParameter)
        modifierRenderer?.renderModifiers(valueParameter)
        if (shouldRenderParameterName(valueParameter)) {

            printer.print(renderParameterName(valueParameter))
            renderReturnTypePrefix()

        }

        renderCallableType(valueParameter)
        renderDefaultValue(valueParameter)
    }

    /**
     * 返回参数名渲染文本。
     */
    protected open fun renderParameterName(valueParameter: CfirValueParameter): String {
        return valueParameter.name.toString()
    }

    /**
     * 判定是否渲染参数名。
     */
    protected open fun shouldRenderParameterName(valueParameter: CfirValueParameter): Boolean {
        return valueParameter.name != SpecialNames.NO_NAME_PROVIDED &&
                valueParameter.containingDeclarationSymbol !is CfirEnumConstructorSymbol
    }

    /**
     * 渲染 callable 的返回类型引用。
     */
    open fun renderCallableType(callableDeclaration: CfirCallableDeclaration) {
        callableDeclaration.returnTypeRef.accept(visitor)
    }

    /**
     * 渲染返回类型前缀。
     */
    open fun renderReturnTypePrefix() {
        printer.print(": ")
    }

    /**
     * 渲染参数默认值。
     */
    protected open fun renderDefaultValue(valueParameter: CfirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ")
            it.accept(visitor)
        }
    }
}
