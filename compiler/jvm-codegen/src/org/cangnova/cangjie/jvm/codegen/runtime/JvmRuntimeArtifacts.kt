package org.cangnova.cangjie.jvm.codegen.runtime

import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.runtime.CjJvmPointerRuntime
import org.cangnova.cangjie.jvm.runtime.CjJvmUnsignedRuntime

object JvmRuntimeArtifacts {
    const val POINTER_RUNTIME_INTERNAL_NAME: String = "org/cangnova/cangjie/jvm/runtime/CjJvmPointerRuntime"
    const val UNSIGNED_RUNTIME_INTERNAL_NAME: String = "org/cangnova/cangjie/jvm/runtime/CjJvmUnsignedRuntime"

    fun pointerRuntimeArtifact(): JvmClassFileArtifact =
        runtimeArtifact(POINTER_RUNTIME_INTERNAL_NAME, CjJvmPointerRuntime::class.java)

    fun unsignedRuntimeArtifact(): JvmClassFileArtifact =
        runtimeArtifact(UNSIGNED_RUNTIME_INTERNAL_NAME, CjJvmUnsignedRuntime::class.java)

    private fun runtimeArtifact(internalName: String, runtimeClass: Class<*>): JvmClassFileArtifact {
        val resourceName = "/$internalName.class"
        val bytes = runtimeClass.getResourceAsStream(resourceName)
            ?.use { it.readBytes() }
            ?: throw JvmCodegenException("missing JVM runtime class resource $resourceName")
        return JvmClassFileArtifact(internalName = internalName, bytes = bytes)
    }
}
