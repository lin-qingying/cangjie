package org.cangnova.cangjie.codegen.context

import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.runtime.RuntimeSymbolTable
import org.cangnova.cangjie.codegen.types.DefaultTypeLowering
import org.cangnova.cangjie.codegen.types.TypeLowering

/**
 * LLVM codegen 阶段的共享上下文。
 */
class CGContext(
    /**
     * 当前 codegen 输入 package。
     */
    val inputPackage: ChirPackage,
    /**
     * 当前 codegen 选项。
     */
    val options: CodegenOptions,
    /**
     * CHIR 类型到 LLVM textual type 的 lowering 服务。
     */
    val typeLowering: TypeLowering = DefaultTypeLowering(),
    /**
     * codegen 可声明和调用的运行时符号表。
     */
    val runtimeSymbols: RuntimeSymbolTable = RuntimeSymbolTable(),
) {
    /**
     * 计算 CHIR module 对应的 LLVM module 名称。
     */
    fun moduleName(module: ChirModule): String = "${inputPackage.name}.${module.name}"
}
