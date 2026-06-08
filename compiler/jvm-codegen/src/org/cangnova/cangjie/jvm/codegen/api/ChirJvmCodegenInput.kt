package org.cangnova.cangjie.jvm.codegen.api

import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * JVM 后端输入。当前阶段以 CHIR 包作为唯一语义来源，避免 JVM 后端反向依赖前端实现细节。
 */
data class ChirJvmCodegenInput(
    val chirPackage: ChirPackage,
    val options: JvmCodegenOptions = JvmCodegenOptions(),
)
