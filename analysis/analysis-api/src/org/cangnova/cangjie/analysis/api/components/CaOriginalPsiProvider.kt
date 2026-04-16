package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

interface CaOriginalPsiProvider : CaLifetimeOwner {
    fun CaSymbol.getOriginalPsi(): PsiElement?
}
