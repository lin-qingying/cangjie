package org.cangnova.cangjie.analysis.api.lightDeclarations

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjFile

data class CaLightDeclarationOrigin(
    val kind: CaLightDeclarationOriginKind,
    val description: String,
    val containingFile: CjFile?,
    val sourceElement: PsiElement?,
)
