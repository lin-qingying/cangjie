package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer

open class CfirDeclarationRenderer(
    private val localVariablePrefix: String = "l",
    private val renderVerboseAccessors: Boolean = false
) {

    internal lateinit var components: CfirRendererComponents
    protected val printer: CfirPrinter get() = components.printer
    private val resolvePhaseRenderer: CfirResolvePhaseRenderer? get() = components.resolvePhaseRenderer
    private val typeRenderer: ConeTypeRenderer get() = components.typeRenderer

    fun render(declaration: CfirDeclaration) {
        renderPhaseAndAttributes(declaration)
        if (declaration is CfirConstructor) {
            declaration.dispatchReceiverType?.let {
                typeRenderer.render(it)
                printer.print(".")
            }
            if (declaration is CfirErrorPrimaryConstructor) {
                printer.print("error_")
            }
            printer.print("constructor")
            return
        }
        printer.print(
            when (declaration) {
                is CfirTypeAlias -> "typealias"
is CfirClass -> "class"
                is CfirEnum -> "enum"
                is CfirStruct -> "struct"
                is CfirInterface -> "interface"
//                is CfirAnonymousFunction -> (declaration.label?.let { "${it.name}@" } ?: "") + "func"
                is CfirNamedFunction -> "func"
                is CfirProperty -> "property"
                is CfirPatternVariable -> {
                    val prefix = if (declaration.isVar) "var" else "let"
                    prefix + " pattern variable"
                }

                else -> "unknown"
            }
        )
    }

    internal fun renderPhaseAndAttributes(declaration: CfirDeclaration) {
        resolvePhaseRenderer?.render(declaration)
        with(declaration) {
            renderDeclarationAttributes()
        }
    }

    protected open fun CfirDeclaration.renderDeclarationAttributes() {
    }
}
