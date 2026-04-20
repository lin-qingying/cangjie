package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 对齐 Kotlin `createOwnerPointer(symbol)` 的统一 owner-pointer 入口。
 */
internal inline fun <reified T : CaDeclarationContainerSymbol> CaSession.createOwnerPointer(symbol: CaDeclarationSymbol): CaSymbolPointer<T> {
    val containingSymbol = symbol.containingDeclaration
        ?: errorWithAttachment("Member declaration `${symbol::class.simpleName}` is missing containing declaration") {}

    val ownerSymbol = containingSymbol as? T
        ?: errorWithAttachment(
            "Unexpected owner `${containingSymbol::class.simpleName}` for member declaration `${symbol::class.simpleName}`",
        ) {}

    @Suppress("UNCHECKED_CAST")
    return ownerSymbol.createPointer() as CaSymbolPointer<T>
}
