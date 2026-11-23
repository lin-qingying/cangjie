package org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * Quick Doc 定义头统一使用的有效 modality 规则。
 *
 * 显式源码修饰符优先；Analysis API 只报告 ABSTRACT 时，接口成员与 abstract class 成员显示为 open。
 */
object CaQuickDocumentationModalityModifierProvider : CaRendererModalityModifierProvider {
    override fun getModalityModifier(symbol: CaDeclarationSymbol): String? {
        val modifierOwner = symbol.psi as? CjModifierListOwner
        when {
            modifierOwner?.hasModifier(CjTokens.ABSTRACT_KEYWORD) == true -> return "abstract"
            modifierOwner?.hasModifier(CjTokens.OPEN_KEYWORD) == true -> return "open"
            modifierOwner?.hasModifier(CjTokens.SEALED_KEYWORD) == true -> return "sealed"
        }

        return when (symbol.modality) {
            CaSymbolModality.OPEN -> "open"
            CaSymbolModality.ABSTRACT -> {
                val containingType = symbol.psi?.let { PsiTreeUtil.getParentOfType(it, CjTypeStatement::class.java) }
                if (
                    containingType is CjInterface ||
                    containingType is CjClass && containingType.hasModifier(CjTokens.ABSTRACT_KEYWORD)
                ) {
                    "open"
                } else {
                    null
                }
            }

            CaSymbolModality.FINAL,
            CaSymbolModality.SEALED,
            null,
            -> null
        }
    }
}
