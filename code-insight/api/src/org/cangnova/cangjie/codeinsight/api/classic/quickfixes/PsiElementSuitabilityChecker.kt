package org.cangnova.cangjie.codeinsight.api.classic.quickfixes

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.lexer.CjModifierKeywordToken

/** 检查指定 PSI 元素是否适合交给 PSI quick-fix factory 处理。 */
fun interface PsiElementSuitabilityChecker<in PSI : PsiElement> {
    fun isSupported(psiElement: PSI): Boolean
}

/** 内建的 PSI 适用性规则。 */
object PsiElementSuitabilityCheckers {
    val ALWAYS_SUITABLE = PsiElementSuitabilityChecker<PsiElement> { true }

    val MODIFIER = PsiElementSuitabilityChecker<LeafPsiElement> { psiElement ->
        psiElement.elementType is CjModifierKeywordToken
    }
}
