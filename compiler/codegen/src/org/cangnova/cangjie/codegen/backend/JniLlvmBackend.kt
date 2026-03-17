package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendUnavailableException
import org.cangnova.cangjie.llvm.jni.LlvmNative

internal interface JniNativeFacade {
    val isAvailable: Boolean
    val diagnostics: String
    val llvmVersion: String?

    fun emitBitcode(moduleName: String, llvmIr: String): ByteArray
}

internal object DefaultJniNativeFacade : JniNativeFacade {
    override val isAvailable: Boolean
        get() = LlvmNative.isAvailable

    override val diagnostics: String
        get() = LlvmNative.loadDiagnostics

    override val llvmVersion: String?
        get() = System.getProperty("cangjie.llvm.version")

    override fun emitBitcode(moduleName: String, llvmIr: String): ByteArray {
        var context = 0L
        var module = 0L
        try {
            context = LlvmNative.contextCreate()
            module = LlvmNative.moduleParseAssemblyInContext(moduleName, llvmIr, context)
            LlvmNative.moduleVerify(module)
            return LlvmNative.writeBitcodeToMemoryBuffer(module)
        } finally {
            if (module != 0L) {
                LlvmNative.moduleDispose(module)
            }
            if (context != 0L) {
                LlvmNative.contextDispose(context)
            }
        }
    }
}

class JniLlvmBackend internal constructor(
    private val native: JniNativeFacade = DefaultJniNativeFacade,
) : LlvmBackend {
    override val id: String = "jni"

    override val capabilities: LlvmBackendCapabilities
        get() = LlvmBackendCapabilities(
            supportsInProcessIR = native.isAvailable,
            supportsOptimization = native.isAvailable,
            supportsTargetCodegen = native.isAvailable,
            llvmVersion = native.llvmVersion,
        )

    override fun initialize() {
        if (!native.isAvailable) {
            throw LlvmBackendUnavailableException(
                "JNI backend is unavailable: ${native.diagnostics}",
            )
        }
    }

    override fun emitBitcode(moduleName: String, llvmIr: String): ByteArray {
        return native.emitBitcode(moduleName, llvmIr)
    }
}
