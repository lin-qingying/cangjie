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

/**
 * JNI LLVM 后端访问原生绑定的隔离门面。
 */
internal interface JniNativeFacade {
    /**
     * 原生 LLVM 绑定当前是否可用。
     */
    val isAvailable: Boolean
    /**
     * 原生绑定加载诊断信息。
     */
    val diagnostics: String
    /**
     * 原生绑定报告的 LLVM 版本。
     */
    val llvmVersion: String?

    /**
     * 安装 LLVM API 绑定。
     */
    fun installApiBindings() = Unit

    /**
     * 发射 LLVM bitcode 字节。
     */
    fun emitBitcode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray

    /**
     * 发射目标 object code 字节。
     */
    fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray

    /**
     * 发射目标 object file。
     */
    fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String)
}

/**
 * 基于真实 `LlvmNative` 和 LLVM API 封装的默认 JNI 原生门面。
 */
internal object DefaultJniNativeFacade : JniNativeFacade {
    /**
     * 读取 `LlvmNative` 当前可用状态。
     */
    override val isAvailable: Boolean
        get() = LlvmNative.isAvailable

    /**
     * 读取 `LlvmNative` 加载诊断。
     */
    override val diagnostics: String
        get() = LlvmNative.loadDiagnostics

    /**
     * 读取 JVM 系统属性中暴露的 LLVM 版本。
     */
    override val llvmVersion: String?
        get() = System.getProperty("cangjie.llvm.version")

    /**
     * 安装 JNI 绑定符号。
     */
    override fun installApiBindings() {
        LlvmNative.installBindings()
    }

    /**
     * 解析 LLVM IR、配置 module、执行可选优化并返回 bitcode。
     */
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

    /**
     * 解析 LLVM IR 并通过 target machine 发射 object code 字节。
     */
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

    /**
     * 解析 LLVM IR 并通过 target machine 写出 object file。
     */
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

    /**
     * 将目标 triple 和 data layout 写入 LLVM module。
     */
    private fun configureModule(module: org.cangnova.cangjie.llvm.api.LlvmModule, options: LlvmBackendEmissionOptions) {
        options.targetTriple?.takeIf { it.isNotBlank() }?.let(module::setTargetTriple)
        options.targetDataLayout?.takeIf { it.isNotBlank() }?.let(module::setDataLayout)
    }

    /**
     * 按配置执行 LLVM module optimization pipeline。
     */
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

/**
 * 基于 JNI 的 LLVM 后端实现。
 */
class JniLlvmBackend internal constructor(
    /**
     * 访问原生 LLVM 能力的门面，测试可注入 fake。
     */
    private val native: JniNativeFacade = DefaultJniNativeFacade,
) : LlvmBackend {
    /**
     * 后端标识。
     */
    override val id: String = "jni"

    /**
     * 根据原生门面的可用状态动态报告后端能力。
     */
    override val capabilities: LlvmBackendCapabilities
        get() = LlvmBackendCapabilities(
            supportsInProcessIR = native.isAvailable,
            supportsOptimization = native.isAvailable,
            supportsTargetCodegen = native.isAvailable,
            llvmVersion = native.llvmVersion,
        )

    /**
     * 初始化 JNI 后端并安装原生 API 绑定。
     */
    override fun initialize() {
        if (!native.isAvailable) {
            throw LlvmBackendUnavailableException(
                "JNI backend is unavailable: ${native.diagnostics}",
            )
        }
        native.installApiBindings()
    }

    /**
     * 委托原生门面发射 bitcode。
     */
    override fun emitBitcode(
        moduleName: String,
        llvmIr: String,
        options: LlvmBackendEmissionOptions,
    ): ByteArray {
        return native.emitBitcode(moduleName, llvmIr, options)
    }

    /**
     * 委托原生门面发射 object code 字节。
     */
    override fun emitObjectCode(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions): ByteArray {
        return native.emitObjectCode(moduleName, llvmIr, options)
    }

    /**
     * 委托原生门面写出 object file。
     */
    override fun emitObjectFile(moduleName: String, llvmIr: String, options: LlvmBackendEmissionOptions, outputPath: String) {
        native.emitObjectFile(moduleName, llvmIr, options, outputPath)
    }
}

/**
 * 将通用后端发射选项转换为 LLVM target machine 选项。
 */
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

/**
 * 将 codegen 优化级别映射到 LLVM API 优化级别。
 */
private fun CodegenOptimizationLevel.toLlvmOptimizationLevel(): LlvmCodeGenOptimizationLevel =
    when (this) {
        CodegenOptimizationLevel.NONE -> LlvmCodeGenOptimizationLevel.NONE
        CodegenOptimizationLevel.LESS -> LlvmCodeGenOptimizationLevel.LESS
        CodegenOptimizationLevel.DEFAULT -> LlvmCodeGenOptimizationLevel.DEFAULT
        CodegenOptimizationLevel.AGGRESSIVE -> LlvmCodeGenOptimizationLevel.AGGRESSIVE
    }

/**
 * 将 codegen 重定位模式映射到 LLVM API 重定位模式。
 */
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

/**
 * 将 codegen code model 映射到 LLVM API code model。
 */
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
