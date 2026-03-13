package org.cangnova.cangjie.chir.core.symbol

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

data class ChirSymbol(
    val semanticId: ChirSemanticId,
    val name: String,
    val declarationId: ChirSemanticId,
)
