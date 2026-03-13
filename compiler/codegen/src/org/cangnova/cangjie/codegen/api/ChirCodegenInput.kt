package org.cangnova.cangjie.codegen.api

import org.cangnova.cangjie.chir.core.model.ChirPackage

data class ChirCodegenInput(
    val chirPackage: ChirPackage,
    val options: CodegenOptions = CodegenOptions(),
)

