package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.source.AbstractCjSourceElement

open class CfirModifierRenderer {
    internal lateinit var components: CfirRendererComponents
    protected val printer: CfirPrinter get() = components.printer

    open fun renderModifiers(declaration: CfirMemberDeclaration) {
        renderVisibility(declaration.status, declaration.source)
        renderModality(declaration.status)
        renderExtraModifiers(declaration.status)
    }

    open fun renderModifiers(extend: CfirExtend) {
    }

    open fun renderModifiers(constructor: CfirConstructor) {
    }

    open fun renderModifiers(valueParameter: CfirValueParameter) {
    }

    protected open fun renderVisibility(status: CfirDeclarationStatus, source: AbstractCjSourceElement?) {
        val visibilityName = status.visibility.name
        if (status.isVisibilityExplicit) {
            renderModifier(visibilityName)
        } else {
            renderModifier("$visibilityName?")
        }
    }

    protected open fun renderModality(status: CfirDeclarationStatus) {
        when {
            status.isAbstract -> renderModifier(if (status.isModalityExplicit) "abstract" else "abstract?")
            status.isOpen    -> renderModifier(if (status.isModalityExplicit) "open"     else "open?")
            status.isSealed  -> renderModifier(if (status.isModalityExplicit) "sealed"   else "sealed?")
        }
    }

    protected open fun renderExtraModifiers(status: CfirDeclarationStatus) {
        if (status.isStatic)    renderModifier("static")
        if (status.isMut)       renderModifier("mut")
        if (status.isOverride)  renderModifier("override")
        if (status.isOperator)  renderModifier("operator")
        if (status.isUnsafe)    renderModifier("unsafe")
        if (status.isForeign)   renderModifier("foreign")
    }

    protected open fun renderModifier(modifier: String) {
        printer.print("$modifier ")
    }
}
