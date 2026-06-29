package org.cangnova.cangjie.jvm.codegen.api

import org.objectweb.asm.Opcodes

/**
 * JVM 后端生成选项。
 */
data class JvmCodegenOptions(
    /**
     * 是否启用 JVM codegen 阶段。
     */
    val enabled: Boolean = true,
    /**
     * lowering 前是否验证 CHIR 形状。
     */
    val validateChirBeforeLowering: Boolean = true,
    /**
     * 生成 class 文件时使用的 JVM class file version。
     */
    val classFileVersion: Int = Opcodes.V17,
    /**
     * 是否为 package-level main 生成 Java main bridge。
     */
    val generateMainBridge: Boolean = true,
    /**
     * 是否在输出中保留 lowering trace。
     */
    val emitLoweringTrace: Boolean = false,
    /**
     * package/module facade class 名称后缀。
     */
    val moduleFacadeSuffix: String = "Cj",
)
