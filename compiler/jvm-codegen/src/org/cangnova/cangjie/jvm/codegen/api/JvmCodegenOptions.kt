package org.cangnova.cangjie.jvm.codegen.api

import org.objectweb.asm.Opcodes

data class JvmCodegenOptions(
    val enabled: Boolean = true,
    val validateChirBeforeLowering: Boolean = true,
    val classFileVersion: Int = Opcodes.V17,
    val generateMainBridge: Boolean = true,
    val emitLoweringTrace: Boolean = false,
    val moduleFacadeSuffix: String = "Cj",
)
