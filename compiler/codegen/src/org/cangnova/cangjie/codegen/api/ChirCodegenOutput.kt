package org.cangnova.cangjie.codegen.api

import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact

data class ChirCodegenOutput(
    val modules: List<LlvmModuleArtifact>,
)

fun interface ChirToLlvmCodeGenerator {
    fun generate(input: ChirCodegenInput): ChirCodegenOutput
}

