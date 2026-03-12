package org.cangjie.cfir.source

import com.intellij.psi.PsiElement
import org.cangjie.cfir.common.CfirSourceElement

sealed interface AbstractCjSourceElement : CfirSourceElement

data class CjPsiSourceElement(
    val psi: PsiElement,
    override val filePath: String? = psi.containingFile?.virtualFile?.path
) : AbstractCjSourceElement {
    override val startOffset: Int get() = psi.textRange.startOffset
    override val endOffset: Int get() = psi.textRange.endOffset
}

data class CjLightSourceElement(
    override val startOffset: Int,
    override val endOffset: Int,
    override val filePath: String?
) : AbstractCjSourceElement
