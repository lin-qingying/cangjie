package org.cangnova.cangjie.codegen.diagnostics

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR 到 LLVM lowering 阶段发现不可生成结构时抛出的异常。
 */
class CodegenLoweringException(
    message: String,
    /**
     * 触发错误的 CHIR 语义 id；没有单一来源节点时为空。
     */
    val semanticId: ChirSemanticId? = null,
) : IllegalArgumentException(message)
