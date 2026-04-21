package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypes2
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry


internal val CjDeclaration.location: CaSymbolLocation
    get() {
        // Note: a declaration can be nested inside a modifier list (for example, in the case of dangling annotations or context parameters)
        val parent = getParentOfTypes2<CjDeclaration, CjModifierList>()

        if (this is CjTypeParameter) {
            return if (parent is CjTypeStatement) CaSymbolLocation.CLASS else CaSymbolLocation.LOCAL
        }

        return when (parent) {
            null -> CaSymbolLocation.TOP_LEVEL

            is CjTypeStatement -> CaSymbolLocation.CLASS

            is CjDeclarationWithBody,
            is CjDeclarationWithInitializer,
            is CjModifierList,
            is CjParameter,
                -> CaSymbolLocation.LOCAL

            else -> errorWithAttachment("Unexpected parent declaration: ${parent::class.simpleName}") {
                withPsiEntry("parentDeclaration", parent)
                withPsiEntry("psi", this@location)
            }
        }
    }

internal fun CaCfirSymbol<*>.findPsi(): PsiElement? {
    return cfirSymbol.findPsi(analysisSession.analysisScope)
}
fun CfirBasedSymbol<*>.findPsi(scope: GlobalSearchScope): PsiElement? {
    return if (
        this is CfirCallableSymbol<*> &&
        !this.isTypeAliasedConstructor // type-aliased constructors should not be unwrapped
    ) {
        cfir.unwrapFakeOverridesOrDelegated().findPsi()
    } else {
        cfir.findPsi()
    } ?: CfirSyntheticFunctionInterfaceSourceProvider.findPsi(cfir, scope)
}
