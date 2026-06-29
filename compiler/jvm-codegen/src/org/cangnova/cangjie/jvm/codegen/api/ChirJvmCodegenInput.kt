package org.cangnova.cangjie.jvm.codegen.api

import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * JVM 后端输入。当前阶段以 CHIR 包作为唯一语义来源，避免 JVM 后端反向依赖前端实现细节。
 */
data class ChirJvmCodegenInput(
    /**
     * 待降低到 JVM class 文件的 CHIR package。
     */
    val chirPackage: ChirPackage,
    /**
     * 本次 JVM codegen 使用的配置选项。
     */
    val options: JvmCodegenOptions = JvmCodegenOptions(),
)
