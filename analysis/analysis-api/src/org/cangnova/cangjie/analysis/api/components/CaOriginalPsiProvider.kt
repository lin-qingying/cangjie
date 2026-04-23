package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.psi.*

interface CaOriginalPsiProvider : CaLifetimeOwner {
    /**
     * If [CjDeclaration] is a non-local declaration in a fake file analyzed in dependent session, returns the original declaration.
     * Otherwise, returns `null`.
     */
    @Deprecated("Obsolete API")
    public fun CjDeclaration.getOriginalDeclaration(): CjDeclaration?

    /**
     * If [this] is a fake file analyzed in dependent session, returns the original file for [this]. Otherwise, returns `null`.
     */
    @Deprecated("Obsolete API")
    public fun CjFile.getOriginalCjFile(): CjFile?

    /**
     * Records [declaration] as an original declaration for [this].
     */
    @Deprecated("Obsolete API")
    public fun CjDeclaration.recordOriginalDeclaration(declaration: CjDeclaration)

    /**
     * Records [file] as an original file for [this].
     */
    @Deprecated("Obsolete API")
    public fun CjFile.recordOriginalCjFile(file: CjFile)
}
