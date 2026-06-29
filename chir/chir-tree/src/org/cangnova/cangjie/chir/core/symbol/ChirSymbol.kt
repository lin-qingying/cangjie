package org.cangnova.cangjie.chir.core.symbol

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR 声明符号。
 */
data class ChirSymbol(
    /**
     * 符号自身语义标识。
     */
    val semanticId: ChirSemanticId,

    /**
     * 符号名称。
     */
    val name: String,

    /**
     * 符号指向的声明语义标识。
     */
    val declarationId: ChirSemanticId,
)
