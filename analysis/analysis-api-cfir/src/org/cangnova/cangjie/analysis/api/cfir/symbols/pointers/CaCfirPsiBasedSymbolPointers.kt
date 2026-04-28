package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjTypeParameter

/**
 * source-only / local 符号的 PSI-based pointer。
 *
 * 这类符号本身没有稳定的全局声明键，Kotlin FIR 侧同样会落到 PSI-based pointer。
 * 因此这里为每种 source-only 公开符号提供专用 pointer，而不是用统一 kind 分发。
 */
internal class CaCfirAnonymousFunctionSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaAnonymousFunctionSymbol>() {
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaAnonymousFunctionSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjFunctionLiteral)?.symbol }
    }
}

internal class CaCfirLocalVariableSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaLocalVariableSymbol>() {
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaLocalVariableSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjPatternVariable)?.symbol as? CaLocalVariableSymbol }
    }
}

internal class CaCfirPatternVariableSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaPatternVariableSymbol>() {
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPatternVariableSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjPatternVariable)?.symbol }
    }
}

internal class CaCfirPatternBindingSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaPatternBindingSymbol>() {
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPatternBindingSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjBindingPattern)?.symbol }
    }
}

internal class CaCfirSourceTypeParameterSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaTypeParameterSymbol>() {
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaTypeParameterSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjTypeParameter)?.symbol }
    }
}

private fun com.intellij.psi.PsiElement.createSmartPointer(): SmartPsiElementPointer<com.intellij.psi.PsiElement> =
    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
