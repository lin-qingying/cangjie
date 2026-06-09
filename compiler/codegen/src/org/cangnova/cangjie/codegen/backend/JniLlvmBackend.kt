package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.api.CodegenCodeModel
import org.cangnova.cangjie.codegen.api.CodegenOptimizationLevel
import org.cangnova.cangjie.codegen.api.CodegenRelocationMode
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendUnavailableException
import org.cangnova.cangjie.llvm.api.LlvmCodeGenOptimizationLevel
import org.cangnova.cangjie.llvm.api.LlvmCodeModel
import org.cangnova.cangjie.llvm.api.LlvmContext
import org.cangnova.cangjie.llvm.api.LlvmPassManager
import org.cangnova.cangjie.llvm.api.LlvmPassPipeline
import org.cangnova.cangjie.llvm.api.LlvmPassManagers
import org.cangnova.cangjie.llvm.api.LlvmRelocationMode
import org.cangnova.cangjie.llvm.api.LlvmTargetMachineOptions
import org.cangnova.cangjie.llvm.api.LlvmTargetMachines
import org.cangnova.cangjie.llvm.jni.LlvmNative

internal interface JniNativeFacade {
    val isAvailable: Boolean
    val diagnostics: String
    val llvmVersion: String?

    fun installApiBindings() = Unit

    fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray

    fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray

    fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String)
}

internal object DefaultJniNativeFacade : JniNativeFacade {
    override val isAvailable: Boolean
        get() = LlvmNative.isAvailable

    override val diagnostics: String
        get() = LlvmNative.loadDiagnostics

    override val llvmVersion: String?
        get() = System.getProperty("cangjie.llvm.version")

    override fun installApiBindings() {
        LlvmNative.installBindings()
    }

    override fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray {
        installApiBindings()
        LlvmContext().use { context ->
            val module = context.parseModule(moduleName, llvmIr)
            configureModule(module, options)
            runOptimizationPasses(module, options)
            module.verify()
            return module.bitcodeBytes()
        }
    }

    override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray {
        installApiBindings()
        LlvmTargetMachines.initializeAll()
        LlvmContext().use { context ->
            val module = context.parseModule(moduleName, llvmIr)
            configureModule(module, options)
            module.verify()
            LlvmTargetMachines.create(options.toTargetMachineOptions()).use { targetMachine ->
                runOptimizationPasses(module, options, targetMachine)
                module.verify()
                return targetMachine.emitObjectBytes(module)
            }
        }
    }

    override fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String) {
        installApiBindings()
        LlvmTargetMachines.initializeAll()
        LlvmContext().use { context ->
            val module = context.parseModule(moduleName, llvmIr)
            configureModule(module, options)
            module.verify()
            LlvmTargetMachines.create(options.toTargetMachineOptions()).use { targetMachine ->
                runOptimizationPasses(module, options, targetMachine)
                module.verify()
                targetMachine.emitObjectFile(module, outputPath)
            }
        }
    }

    private fun configureModule(module: org.cangnova.cangjie.llvm.api.LlvmModule, options: LlvmBackendEmissionOptions) {
        options.targetTriple?.takeIf { it.isNotBlank() }?.let(module::setTargetTriple)
        options.targetDataLayout?.takeIf { it.isNotBlank() }?.let(module::setDataLayout)
    }

    private fun runOptimizationPasses(
        module: org.cangnova.cangjie.llvm.api.LlvmModule,
        options: LlvmBackendEmissionOptions,
        targetMachine: org.cangnova.cangjie.llvm.api.LlvmTargetMachine? = null,
    ) {
        if (!options.optimizeModule) return
        LlvmPassManagers.createModulePassManager(
            pipeline = LlvmPassPipeline.defaultOptimization(options.optimizationLevel.toLlvmOptimizationLevel()),
            targetMachine = targetMachine,
        ).use { passManager: LlvmPassManager ->
            passManager.run(module)
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
        native.installApiBindings()
    }

    override fun emitBitcode(
        moduleName: String,
        llvmIr: String,
        options: LlvmBackendEmissionOptions,
    ): ByteArray {
        return native.emitBitcode(moduleName, llvmIr, options)
    }

    override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray {
        return native.emitObjectCode(moduleName, llvmIr, options)
    }

    override fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String) {
        native.emitObjectFile(moduleName, llvmIr, options, outputPath)
    }
}

private fun LlvmBackendEmissionOptions.toTargetMachineOptions(): LlvmTargetMachineOptions =
    LlvmTargetMachineOptions(
        targetTriple = targetTriple?.takeIf { it.isNotBlank() }
            ?: throw LlvmBackendUnavailableException("LLVM object emission requires a non-empty target triple"),
        cpu = targetCpu,
        features = targetFeatures,
        optimizationLevel = optimizationLevel.toLlvmOptimizationLevel(),
        relocationMode = relocationMode.toLlvmRelocationMode(),
        codeModel = codeModel.toLlvmCodeModel(),
    )

private fun CodegenOptimizationLevel.toLlvmOptimizationLevel(): LlvmCodeGenOptimizationLevel =
    when (this) {
        CodegenOptimizationLevel.NONE -> LlvmCodeGenOptimizationLevel.NONE
        CodegenOptimizationLevel.LESS -> LlvmCodeGenOptimizationLevel.LESS
        CodegenOptimizationLevel.DEFAULT -> LlvmCodeGenOptimizationLevel.DEFAULT
        CodegenOptimizationLevel.AGGRESSIVE -> LlvmCodeGenOptimizationLevel.AGGRESSIVE
    }

private fun CodegenRelocationMode.toLlvmRelocationMode(): LlvmRelocationMode =
    when (this) {
        CodegenRelocationMode.DEFAULT -> LlvmRelocationMode.DEFAULT
        CodegenRelocationMode.STATIC -> LlvmRelocationMode.STATIC
        CodegenRelocationMode.PIC -> LlvmRelocationMode.PIC
        CodegenRelocationMode.DYNAMIC_NO_PIC -> LlvmRelocationMode.DYNAMIC_NO_PIC
        CodegenRelocationMode.ROPI -> LlvmRelocationMode.ROPI
        CodegenRelocationMode.RWPI -> LlvmRelocationMode.RWPI
        CodegenRelocationMode.ROPI_RWPI -> LlvmRelocationMode.ROPI_RWPI
    }

private fun CodegenCodeModel.toLlvmCodeModel(): LlvmCodeModel =
    when (this) {
        CodegenCodeModel.DEFAULT -> LlvmCodeModel.DEFAULT
        CodegenCodeModel.JIT_DEFAULT -> LlvmCodeModel.JIT_DEFAULT
        CodegenCodeModel.TINY -> LlvmCodeModel.TINY
        CodegenCodeModel.SMALL -> LlvmCodeModel.SMALL
        CodegenCodeModel.KERNEL -> LlvmCodeModel.KERNEL
        CodegenCodeModel.MEDIUM -> LlvmCodeModel.MEDIUM
        CodegenCodeModel.LARGE -> LlvmCodeModel.LARGE
    }
