package org.cangnova.cangjie.jvm.codegen.runtime

import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.runtime.CjJvmPointerRuntime
import org.cangnova.cangjie.jvm.runtime.CjJvmUnsignedRuntime

/**
 * JVM 后端需要随生成产物一起输出的运行时 class artifact。
 */
object JvmRuntimeArtifacts {
    /**
     * 指针运行时 class 的 JVM internal name。
     */
    const val POINTER_RUNTIME_INTERNAL_NAME: String = "org/cangnova/cangjie/jvm/runtime/CjJvmPointerRuntime"
    /**
     * 无符号数运行时 class 的 JVM internal name。
     */
    const val UNSIGNED_RUNTIME_INTERNAL_NAME: String = "org/cangnova/cangjie/jvm/runtime/CjJvmUnsignedRuntime"

    /**
     * 读取指针运行时 class 文件 artifact。
     */
    fun pointerRuntimeArtifact(): JvmClassFileArtifact =
        runtimeArtifact(POINTER_RUNTIME_INTERNAL_NAME, CjJvmPointerRuntime::class.java)

    /**
     * 读取无符号数运行时 class 文件 artifact。
     */
    fun unsignedRuntimeArtifact(): JvmClassFileArtifact =
        runtimeArtifact(UNSIGNED_RUNTIME_INTERNAL_NAME, CjJvmUnsignedRuntime::class.java)

    /**
     * 从当前 classpath 中读取指定运行时 class 的字节。
     */
    private fun runtimeArtifact(internalName: String, runtimeClass: Class<*>): JvmClassFileArtifact {
        val resourceName = "/$internalName.class"
        val bytes = runtimeClass.getResourceAsStream(resourceName)
            ?.use { it.readBytes() }
            ?: throw JvmCodegenException("missing JVM runtime class resource $resourceName")
        return JvmClassFileArtifact(internalName = internalName, bytes = bytes)
    }
}
