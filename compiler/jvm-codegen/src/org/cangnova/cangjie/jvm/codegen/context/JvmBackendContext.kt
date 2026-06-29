package org.cangnova.cangjie.jvm.codegen.context

import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.jvm.codegen.api.JvmCodegenOptions
import org.cangnova.cangjie.jvm.codegen.naming.JvmNamePolicy
import org.cangnova.cangjie.jvm.codegen.types.JvmTypeMapper

/**
 * JVM 后端上下文。集中持有包级输入、命名策略和类型映射，避免 class/function codegen 自行拼接 ABI。
 */
class JvmBackendContext(
    /**
     * 当前 JVM lowering 的 CHIR package 输入。
     */
    val inputPackage: ChirPackage,
    /**
     * 当前 JVM lowering 使用的选项。
     */
    val options: JvmCodegenOptions,
    /**
     * JVM 名称生成策略。
     */
    val namePolicy: JvmNamePolicy = JvmNamePolicy(options),
    /**
     * CHIR 类型到 JVM ASM 类型的映射器。
     */
    val typeMapper: JvmTypeMapper = JvmTypeMapper(inputPackage.name),
)
