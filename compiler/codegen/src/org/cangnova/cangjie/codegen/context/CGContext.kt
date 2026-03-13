package org.cangnova.cangjie.codegen.context

import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.runtime.RuntimeSymbolTable
import org.cangnova.cangjie.codegen.types.DefaultTypeLowering
import org.cangnova.cangjie.codegen.types.TypeLowering

class CGContext(
    val inputPackage: ChirPackage,
    val options: CodegenOptions,
    val typeLowering: TypeLowering = DefaultTypeLowering(),
    val runtimeSymbols: RuntimeSymbolTable = RuntimeSymbolTable(),
) {
    fun moduleName(module: ChirModule): String = "${inputPackage.name}.${module.name}"
}

