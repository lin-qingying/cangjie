package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.api.LlvmBackendKind
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendException

class LlvmBackendFactory(
    private val toolRunner: NativeInteropToolRunner = ProcessNativeInteropToolRunner(),
    private val toolLocator: NativeInteropToolLocator = NativeInteropToolLocator(),
) {
    fun createAndInitialize(options: CodegenOptions): LlvmBackendApi {
        return when (options.llvmBackendKind) {
            LlvmBackendKind.IN_MEMORY -> InMemoryLlvmBackendApi().also(LlvmBackendApi::initialize)
            LlvmBackendKind.NATIVE_INTEROP -> createNativeInterop(options)
        }
    }

    private fun createNativeInterop(options: CodegenOptions): LlvmBackendApi {
        val resolvedTool = toolLocator.resolve(options.nativeInteropTool)
        val backend = NativeInteropLlvmBackendApi(
            tool = resolvedTool,
            requiredMajorVersion = options.requiredLlvmMajorVersion,
            runner = toolRunner,
        )
        return try {
            backend.initialize()
            backend
        } catch (error: LlvmBackendException) {
            if (options.nativeInteropFailOnUnavailable) {
                throw error
            }
            InMemoryLlvmBackendApi().also(LlvmBackendApi::initialize)
        }
    }
}
