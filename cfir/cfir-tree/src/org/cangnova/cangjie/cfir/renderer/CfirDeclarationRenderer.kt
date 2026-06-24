package org.cangnova.cangjie.cfir.renderer

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.render.ConeTypeRenderer

/**
 * CFIR 声明头渲染器。
 *
 * 负责输出声明 resolve phase、属性标记以及声明种类文本。
 *
 * @property localVariablePrefix 局部变量渲染前缀。
 * @property renderVerboseAccessors 是否以详细形式渲染 property accessor。
 */
open class CfirDeclarationRenderer(
    private val localVariablePrefix: String = "l",
    private val renderVerboseAccessors: Boolean = false
) {

    /**
     * 当前 renderer 共享组件。
     */
    internal lateinit var components: CfirRendererComponents

    /**
     * 当前输出 printer。
     */
    protected val printer: CfirPrinter get() = components.printer

    /**
     * resolve phase 渲染器。
     */
    private val resolvePhaseRenderer: CfirResolvePhaseRenderer? get() = components.resolvePhaseRenderer

    /**
     * cone 类型渲染器。
     */
    private val typeRenderer: ConeTypeRenderer get() = components.typeRenderer

    /**
     * 渲染声明种类与必要前缀。
     */
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

    /**
     * 渲染声明的 resolve phase 与扩展属性。
     */
    internal fun renderPhaseAndAttributes(declaration: CfirDeclaration) {
        resolvePhaseRenderer?.render(declaration)
        with(declaration) {
            renderDeclarationAttributes()
        }
    }

    /**
     * 渲染声明附加属性。
     */
    protected open fun CfirDeclaration.renderDeclarationAttributes() {
    }
}
