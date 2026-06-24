package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * CFIR 声明修饰符渲染器。
 */
open class CfirModifierRenderer {
    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前输出 printer。
     */
    protected val printer: CfirPrinter get() = components.printer

    /**
     * 渲染成员声明的可见性、modality 与额外修饰符。
     */
    open fun renderModifiers(declaration: CfirMemberDeclaration) {
        renderVisibility(declaration.status, declaration.source)
        renderModality(declaration.status)
        renderExtraModifiers(declaration.status)
    }

    /**
     * 渲染 extend 声明修饰符。
     */
    open fun renderModifiers(extend: CfirExtend) {
    }

    /**
     * 渲染构造器修饰符。
     */
    open fun renderModifiers(constructor: CfirConstructor) {
    }

    /**
     * 渲染值参数修饰符。
     */
    open fun renderModifiers(valueParameter: CfirValueParameter) {
    }

    /**
     * 渲染可见性修饰符。
     */
    protected open fun renderVisibility(status: CfirDeclarationStatus, source: AbstractCjSourceElement?) {
        val visibilityName = status.visibility.name
        if (status.isVisibilityExplicit) {
            renderModifier(visibilityName)
        } else {
            renderModifier("$visibilityName?")
        }
    }

    /**
     * 渲染 modality 修饰符。
     */
    protected open fun renderModality(status: CfirDeclarationStatus) {
        when {
            status.isAbstract -> renderModifier(if (status.isModalityExplicit) "abstract" else "abstract?")
            status.isOpen    -> renderModifier(if (status.isModalityExplicit) "open"     else "open?")
            status.isSealed  -> renderModifier(if (status.isModalityExplicit) "sealed"   else "sealed?")
        }
    }

    /**
     * 渲染仓颉额外声明修饰符。
     */
    protected open fun renderExtraModifiers(status: CfirDeclarationStatus) {
        if (status.isStatic)    renderModifier("static")
        if (status.isMut)       renderModifier("mut")
        if (status.isOverride)  renderModifier("override")
        if (status.isOperator)  renderModifier("operator")
        if (status.isUnsafe)    renderModifier("unsafe")
        if (status.isForeign)   renderModifier("foreign")
    }

    /**
     * 输出单个修饰符文本。
     */
    protected open fun renderModifier(modifier: String) {
        printer.print("$modifier ")
    }
}
