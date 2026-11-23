package org.cangnova.cangjie.wasm.codegen.diagnostics

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/** Wasm backend 无法保持 CHIR 语义或 module 契约时抛出的结构化失败。 */
class WasmCodegenException(
    message: String,
    /** 出错 CHIR 节点；编码层错误没有对应 CHIR 节点时为空。 */
    val semanticId: ChirSemanticId? = null,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
