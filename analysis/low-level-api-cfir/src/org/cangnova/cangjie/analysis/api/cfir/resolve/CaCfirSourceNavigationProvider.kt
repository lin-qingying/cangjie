package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.psi

/**
 * use-site 模块闭包上的 low-level 源码导航入口。
 *
 * 这层统一承载“底层符号 -> 源码 PSI / 源码文件”的映射规则，避免上层组件继续各自维护：
 * 1. 先尝试读取 bound CFIR source。
 * 2. 失败后再按 `ClassId` / `CallableId` 回查源码声明。
 *
 * 对 Analysis API 而言，这是一条稳定的低层协议；具体采用哪种回查策略属于 low-level 实现细节。
 */
internal class CaCfirSourceNavigationProvider(
    private val moduleResolveComponents: CaCfirModuleResolveComponents,
) {
    private val declarationLocator: CaCfirDeclarationLocator
        get() = moduleResolveComponents.declarationLocator

    /**
     * 查找底层符号对应的源码 PSI。
     */
    fun findPsi(symbol: CfirSymbol<*>): PsiElement? {
        val boundPsi = symbol.boundPsiOrNull()
        if (boundPsi != null) {
            return boundPsi
        }

        return when (symbol) {
            is CfirFileSymbol -> symbol.cfir.source?.psi as? CjFile
            is CfirClassLikeSymbol<*> -> declarationLocator.findClassLikeDeclaration(symbol.classId)
            is CfirCallableSymbol<*> -> symbol.callableId?.let(declarationLocator::findCallableDeclaration)
            else -> null
        }
    }

    /**
     * 查找底层符号所属的源码文件。
     */
    fun getContainingFile(symbol: CfirSymbol<*>): CjFile? {
        return when (val psi = findPsi(symbol)) {
            is CjFile -> psi
            is CjCallableDeclaration -> psi.containingFile as? CjFile
            is CjClassLikeDeclaration -> psi.containingFile as? CjFile
            else -> psi?.containingFile as? CjFile
        }
    }

    private fun CfirSymbol<*>.boundPsiOrNull(): PsiElement? {
        return if (isBound) cfir.source?.psi else null
    }
}
