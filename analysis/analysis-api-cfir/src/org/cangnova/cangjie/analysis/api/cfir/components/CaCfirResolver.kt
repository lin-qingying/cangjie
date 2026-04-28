package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.references.CaCfirReference
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCallInfo
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.idea.references.mainReference
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * CFIR resolver 组件。
 *
 * 该组件只负责把公开 Analysis API 的解析请求映射到 session 内部协议，
 * 不再直接接触 low-level facade。
 */
internal class CaCfirResolver(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaResolver, CaCfirSessionComponent {
    override fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        val directSymbols = doResolveToSymbols(this)
        if (directSymbols.isNotEmpty()) {
            return@withValidityAssertion directSymbols
        }

        val branchBindings = restoreMatchBranchPatternBindings(this)
        if (branchBindings.isNotEmpty()) {
            return@withValidityAssertion branchBindings.distinctSymbols()
        }

        return@withValidityAssertion emptyList()
    }

    private fun doResolveToSymbols(referenceExpression: CjReferenceExpression): Collection<CaSymbol> {
        val reference = referenceExpression.mainReference ?: return emptyList()
        checkWithAttachment(
            reference is CaCfirReference,
            { "${reference::class.simpleName} is not extends ${CaCfirReference::class.simpleName}" },
        ) {
            withPsiEntry("reference", reference.element)
        }

        with(reference) {
            return analysisSession.resolveToSymbols()
        }
    }

    override fun CjElement.resolveToCall(): CaCallInfo? = withValidityAssertion {
        null
    }

    /**
     * `match` 分支中的模式绑定属于源码局部声明。
     *
     * 它们在当前仓库里还没有完全通过 low-level reference 索引稳定暴露，
     * 但其语义边界在 PSI 上是明确的：只能解析到当前分支条件侧声明的具名绑定。
     * 因此这里直接基于 `CjMatchEntry.conditions` 恢复同分支 binding symbol，
     * 保证不同分支的同名绑定不会混淆。
     */
    private fun restoreMatchBranchPatternBindings(reference: CjReferenceExpression): Collection<CaSymbol> {
        val simpleName = reference as? CjSimpleNameExpression ?: return emptyList()
        val matchEntry = simpleName.getStrictParentOfType<CjMatchEntry>() ?: return emptyList()
        val arrow = matchEntry.arrow ?: return emptyList()
        if (simpleName.textOffset <= arrow.textOffset) {
            return emptyList()
        }

        return matchEntry.conditions.asSequence()
            .flatMap { condition ->
                sequence {
                    yieldAll(condition.getAllBindings().asSequence())
                    yieldAll(com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(condition, CjVarOrEnumPattern::class.java).asSequence())
                }
            }
            .filter { declaration -> declaration.name == simpleName.referencedName }
            .mapNotNull { declaration ->
                resolvePatternBindingSymbolByPsi(declaration)
                    ?: (declaration as? CjVarOrEnumPattern)?.reference?.let(::resolvePatternBindingSymbolByPsi)
            }
            .toList()
    }

    private fun Collection<CaSymbol>.distinctSymbols(): List<CaSymbol> {
        return distinctBy { symbol ->
            symbol.publicSymbolCacheKeyOrNull() ?: "${symbol::class.qualifiedName}@${System.identityHashCode(symbol)}"
        }
    }

    private fun resolvePatternBindingSymbolByPsi(psi: com.intellij.psi.PsiElement): CaPatternBindingSymbol? {
        val cfirDeclaration = (psi as? CjElement)
            ?.getOrBuildCfir(analysisSession.resolutionFacade) as? CfirDeclaration
            ?: return null
        return listOf(buildPublicSymbol(cfirDeclaration.symbol))
            .filterIsInstance<CaPatternBindingSymbol>()
            .firstOrNull()
    }

    private fun buildPublicSymbol(symbol: CfirBasedSymbol<*>): CaSymbol {
        return analysisSession.cfirSymbolBuilder.buildSymbol(symbol)
    }
}
