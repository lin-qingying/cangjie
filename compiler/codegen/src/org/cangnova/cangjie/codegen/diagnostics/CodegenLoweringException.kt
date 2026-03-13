package org.cangnova.cangjie.codegen.diagnostics

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

class CodegenLoweringException(
    message: String,
    val semanticId: ChirSemanticId? = null,
) : IllegalArgumentException(message)

