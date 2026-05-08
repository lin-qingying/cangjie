package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.psi.CjDeclarationWithInitializer

/**
 * 变量初始化器 renderer。
 *
 * Kotlin 只有 `NO_INITIALIZER` / 常量初始化器；仓颉 source preset 在同一 slot
 * 上补充真实源码与占位文本两种输出。
 */
fun interface CaVariableInitializerRenderer {
    fun renderInitializer(
        analysisSession: CaSession,
        symbol: CaVariableSymbol,
        printer: PrettyPrinter,
    )

    companion object {
        val NO_INITIALIZER: CaVariableInitializerRenderer = CaVariableInitializerRenderer { _, _, _ -> }

        val AS_SOURCE: CaVariableInitializerRenderer = CaVariableInitializerRenderer { _, symbol, printer ->
            val declaration = symbol.findDeclarationWithInitializer() ?: return@CaVariableInitializerRenderer
            declaration.initializer?.let { initializer ->
                printer.append(" = ${initializer.text}")
            }
        }

        val AS_PLACEHOLDER: CaVariableInitializerRenderer = CaVariableInitializerRenderer { _, symbol, printer ->
            val declaration = symbol.findDeclarationWithInitializer() ?: return@CaVariableInitializerRenderer
            if (declaration.initializer != null) {
                printer.append(" = ...")
            }
        }

        /**
         * 仓颉局部绑定符号常常挂在 `CjBindingPattern` 上，初始化器位于外围变量声明。
         *
         * 因此 source/placeholder initializer 不能只看 `symbol.psi` 自身，
         * 还要向上恢复最近的 `CjDeclarationWithInitializer`。
         */
        private fun CaVariableSymbol.findDeclarationWithInitializer(): CjDeclarationWithInitializer? {
            val psi = psi ?: return null
            return (psi as? CjDeclarationWithInitializer)
                ?: PsiTreeUtil.getParentOfType(psi, CjDeclarationWithInitializer::class.java, false)
        }
    }
}
