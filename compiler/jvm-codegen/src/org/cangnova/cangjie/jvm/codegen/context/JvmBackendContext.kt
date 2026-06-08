package org.cangnova.cangjie.jvm.codegen.context

import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.jvm.codegen.api.JvmCodegenOptions
import org.cangnova.cangjie.jvm.codegen.naming.JvmNamePolicy
import org.cangnova.cangjie.jvm.codegen.types.JvmTypeMapper

/**
 * JVM 后端上下文。集中持有包级输入、命名策略和类型映射，避免 class/function codegen 自行拼接 ABI。
 */
class JvmBackendContext(
    val inputPackage: ChirPackage,
    val options: JvmCodegenOptions,
    val namePolicy: JvmNamePolicy = JvmNamePolicy(options),
    val typeMapper: JvmTypeMapper = JvmTypeMapper(inputPackage.name),
)
