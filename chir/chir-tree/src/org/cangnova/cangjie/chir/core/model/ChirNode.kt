package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * 所有 CHIR 模型节点的基础接口。
 */
interface ChirNode {
    /**
     * 节点稳定语义标识。
     */
    val semanticId: ChirSemanticId
}
