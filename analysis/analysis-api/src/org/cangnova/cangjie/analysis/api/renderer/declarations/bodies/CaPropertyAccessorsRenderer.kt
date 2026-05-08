package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol

fun interface CaPropertyAccessorsRenderer {
    fun renderAccessors(
        analysisSession: CaSession,
        symbol: CaPropertySymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /**
         * 对齐 Kotlin `ALL`：getter/setter 只通过各自 renderer 输出。
         */
        val ALL: CaPropertyAccessorsRenderer = CaPropertyAccessorsRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                val toRender = listOfNotNull(symbol.getter, symbol.setter).ifEmpty { return@CaPropertyAccessorsRenderer }
                append("\n")
                withIndent {
                    "\n".separated(
                        {
                            toRender.filterIsInstance<CaPropertyGetterSymbol>().firstOrNull()?.let { getter ->
                                declarationRenderer.getterRenderer.renderSymbol(analysisSession, getter, declarationRenderer, printer)
                            }
                        },
                        {
                            toRender.filterIsInstance<CaPropertySetterSymbol>().firstOrNull()?.let { setter ->
                                declarationRenderer.setterRenderer.renderSymbol(analysisSession, setter, declarationRenderer, printer)
                            }
                        },
                    )
                }
            }
        }

        /**
         * 对齐 Kotlin `NO_DEFAULT`：默认访问器不打印，显式访问器或带注解访问器打印。
         */
        val NO_DEFAULT: CaPropertyAccessorsRenderer = CaPropertyAccessorsRenderer { analysisSession, symbol, declarationRenderer, printer ->
            printer {
                val toRender = listOfNotNull(symbol.getter, symbol.setter)
                    .filter { accessor -> !accessor.isDefault || accessor.annotations.isNotEmpty() }
                    .ifEmpty { return@CaPropertyAccessorsRenderer }
                append("\n")
                withIndent {
                    "\n".separated(
                        {
                            toRender.filterIsInstance<CaPropertyGetterSymbol>().firstOrNull()?.let { getter ->
                                declarationRenderer.getterRenderer.renderSymbol(analysisSession, getter, declarationRenderer, printer)
                            }
                        },
                        {
                            toRender.filterIsInstance<CaPropertySetterSymbol>().firstOrNull()?.let { setter ->
                                declarationRenderer.setterRenderer.renderSymbol(analysisSession, setter, declarationRenderer, printer)
                            }
                        },
                    )
                }
            }
        }

        val NONE: CaPropertyAccessorsRenderer = CaPropertyAccessorsRenderer { _, _, _, _ -> }
    }
}
