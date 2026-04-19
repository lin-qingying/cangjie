package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.name.SpecialNames

open class CfirCallableSignatureRenderer {
    internal lateinit var components: CfirRendererComponents
    protected val printer: CfirPrinter get() = components.printer
    protected val visitor: CfirRenderer.Visitor get() = components.visitor
    private val annotationRenderer: CfirAnnotationRenderer? get() = components.annotationRenderer
    protected val declarationRenderer: CfirDeclarationRenderer? get() = components.declarationRenderer
    private val modifierRenderer: CfirModifierRenderer? get() = components.modifierRenderer
    protected val typeRenderer: ConeTypeRenderer get() = components.typeRenderer

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

    fun renderParameter(valueParameter: CfirValueParameter) {
        declarationRenderer?.renderPhaseAndAttributes(valueParameter)
        annotationRenderer?.render(valueParameter)
        modifierRenderer?.renderModifiers(valueParameter)
        if (valueParameter.name != SpecialNames.NO_NAME_PROVIDED) {

                printer.print(renderParameterName(valueParameter))
                renderReturnTypePrefix()

        }

        renderCallableType(valueParameter)
        renderDefaultValue(valueParameter)
    }

    protected open fun renderParameterName(valueParameter: CfirValueParameter): String {
        return valueParameter.name.toString()
    }

    open fun renderCallableType(callableDeclaration: CfirCallableDeclaration) {
        callableDeclaration.returnTypeRef.accept(visitor)
    }

    open fun renderReturnTypePrefix() {
        printer.print(": ")
    }

    protected open fun renderDefaultValue(valueParameter: CfirValueParameter) {
        valueParameter.defaultValue?.let {
            printer.print(" = ")
            it.accept(visitor)
        }
    }
}
