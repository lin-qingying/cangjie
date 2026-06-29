package org.cangnova.cangjie.chir.core.symbol

import org.cangnova.cangjie.chir.core.builder.BoundChirReference
import org.cangnova.cangjie.chir.core.builder.UnboundChirReference

/**
 * 基于符号表的 CHIR 引用绑定器。
 */
class ChirReferenceBinder(
    /**
     * 用于解析目标名称的符号表。
     */
    private val symbolTable: ChirSymbolTable,
) {
    /**
     * 将未绑定引用解析为目标声明引用。
     */
    fun bind(reference: UnboundChirReference): BoundChirReference? {
        val symbol = symbolTable.resolveByName(reference.targetName) ?: return null
        return BoundChirReference(
            referenceId = reference.referenceId,
            targetDeclarationId = symbol.declarationId,
        )
    }
}
