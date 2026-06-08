package org.cangnova.cangjie.jvm.codegen.diagnostics

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

open class JvmCodegenException(
    message: String,
    val nodeId: ChirSemanticId? = null,
    cause: Throwable? = null,
) : IllegalStateException(
    if (nodeId == null) message else "$message at ${nodeId.value}",
    cause,
)
