package org.cangnova.cangjie.jvm.codegen.diagnostics

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * JVM 后端在 CHIR lowering 或 class 生成阶段报告的结构化异常。
 */
open class JvmCodegenException(
    message: String,
    /**
     * 触发错误的 CHIR 节点 ID；无法定位时为空。
     */
    val nodeId: ChirSemanticId? = null,
    cause: Throwable? = null,
) : IllegalStateException(
    if (nodeId == null) message else "$message at ${nodeId.value}",
    cause,
)
