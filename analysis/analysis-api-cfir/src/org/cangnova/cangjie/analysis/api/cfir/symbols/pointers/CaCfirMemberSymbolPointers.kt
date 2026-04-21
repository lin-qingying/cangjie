package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession

import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin `KaFirMemberSymbolPointer` 的 member symbol pointer 基座。
 *
 * 当前仓颉主干不区分 JVM static/member scope，也没有 Java interop，
 * 因此这里只保留 owner pointer + member 选择这一条主恢复链。
 */
internal abstract class CaCfirMemberSymbolPointer<S : CaSymbol>(
    private val ownerPointer: CaSymbolPointer<CaDeclarationContainerSymbol>,
) : CaCfirSymbolPointerBase<S>() {
    @CaImplementationDetail
    final override fun restoreSymbol(session: CaSession): S? {
        val cfirSession = restoreSession(session) ?: return null
        val ownerSymbol = ownerPointer.restoreSymbol(session) ?: return null
        return cfirSession.chooseCandidateAndCreateSymbol(ownerSymbol)
    }

    protected abstract fun CaCfirSession.chooseCandidateAndCreateSymbol(ownerSymbol: CaDeclarationContainerSymbol): S?
}

@OptIn(CaImplementationDetail::class)
internal class CaCfirMemberFunctionSymbolPointer(
    private val ownerPointer: CaSymbolPointer<CaDeclarationContainerSymbol>,
    private val name: Name,
    private val signature: CfirCallableSignature,
) : CaCfirMemberSymbolPointer<CaNamedFunctionSymbol>(ownerPointer) {
    @OptIn(CaImplementationDetail::class)
    override fun CaCfirSession.chooseCandidateAndCreateSymbol(ownerSymbol: CaDeclarationContainerSymbol): CaNamedFunctionSymbol? {
        val candidates = when (ownerSymbol) {
            is org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol -> with(this) {
                ownerSymbol.declaredMemberScope.getCallableSymbols(name)
            }

            is org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol -> with(this) {
                ownerSymbol.declaredMemberScope.getCallableSymbols(name)
            }

            else -> emptyList()
        }

        return candidates
            .filterIsInstance<CaNamedFunctionSymbol>()
            .singleOrNull { candidate ->
                val backingSymbol = (candidate as? CaCfirBackedSymbol<*>)?.backingSymbol
                signature.hasTheSameSignature(
                    backingSymbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*> ?: return@singleOrNull false
                )
            }
    }
}
