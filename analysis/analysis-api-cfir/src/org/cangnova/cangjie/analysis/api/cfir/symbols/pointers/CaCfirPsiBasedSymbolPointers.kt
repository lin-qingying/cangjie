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
    /**
     * 匿名函数源码 PSI 的 IntelliJ smart pointer。
     */
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    /**
     * 从 smart pointer 恢复函数 literal 并返回匿名函数符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaAnonymousFunctionSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjFunctionLiteral)?.symbol }
    }
}

/**
 * 局部变量符号 pointer。
 */
internal class CaCfirLocalVariableSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaLocalVariableSymbol>() {
    /**
     * 局部变量源码 PSI 的 IntelliJ smart pointer。
     */
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    /**
     * 从 smart pointer 恢复 pattern variable 并返回局部变量符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaLocalVariableSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjPatternVariable)?.symbol as? CaLocalVariableSymbol }
    }
}

/**
 * 模式变量符号 pointer。
 */
internal class CaCfirPatternVariableSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaPatternVariableSymbol>() {
    /**
     * 模式变量源码 PSI 的 IntelliJ smart pointer。
     */
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    /**
     * 从 smart pointer 恢复 pattern variable 并返回模式变量符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPatternVariableSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjPatternVariable)?.symbol }
    }
}

/**
 * 模式绑定符号 pointer。
 */
internal class CaCfirPatternBindingSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaPatternBindingSymbol>() {
    /**
     * 模式绑定源码 PSI 的 IntelliJ smart pointer。
     */
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    /**
     * 从 smart pointer 恢复 binding pattern 并返回模式绑定符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaPatternBindingSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjBindingPattern)?.symbol }
    }
}

/**
 * 源码类型参数符号 pointer。
 */
internal class CaCfirSourceTypeParameterSymbolPointer(
    psi: com.intellij.psi.PsiElement,
) : CaCfirSymbolPointerBase<CaTypeParameterSymbol>() {
    /**
     * 类型参数源码 PSI 的 IntelliJ smart pointer。
     */
    private val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement> = psi.createSmartPointer()

    /**
     * 从 smart pointer 恢复类型参数 PSI 并返回类型参数符号。
     */
    override fun restoreSymbol(session: org.cangnova.cangjie.analysis.api.CaSession): CaTypeParameterSymbol? {
        val cfirSession = restoreSession(session) ?: return null
        val psi = pointer.element ?: return null
        return with(cfirSession) { (psi as? CjTypeParameter)?.symbol }
    }
}

/**
 * 为 PSI 元素创建工程绑定的 smart pointer。
 */
private fun com.intellij.psi.PsiElement.createSmartPointer(): SmartPsiElementPointer<com.intellij.psi.PsiElement> =
    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
