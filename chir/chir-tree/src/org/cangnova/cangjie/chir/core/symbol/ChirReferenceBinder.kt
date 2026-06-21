package org.cangnova.cangjie.chir.core.symbol

import org.cangnova.cangjie.chir.core.builder.BoundChirReference
import org.cangnova.cangjie.chir.core.builder.UnboundChirReference

class ChirReferenceBinder(
    private val symbolTable: ChirSymbolTable,
) {
    fun bind(reference: UnboundChirReference): BoundChirReference? {
        val symbol = symbolTable.resolveByName(reference.targetName) ?: return null
        return BoundChirReference(
            referenceId = reference.referenceId,
            targetDeclarationId = symbol.declarationId,
        )
    }
}
