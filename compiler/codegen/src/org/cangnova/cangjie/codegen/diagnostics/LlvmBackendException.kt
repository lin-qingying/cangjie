package org.cangnova.cangjie.codegen.diagnostics

/**
 * LLVM 后端调用失败的基类异常。
 */
open class LlvmBackendException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * 请求原生 LLVM 产物但后端不可用时的异常。
 */
class LlvmBackendUnavailableException(
    message: String,
    cause: Throwable? = null,
) : LlvmBackendException(message, cause)

/**
 * LLVM 后端版本与 codegen 配置要求不一致时的异常。
 */
class LlvmBackendVersionMismatchException(
    /**
     * 配置要求的 LLVM 主版本。
     */
    val expectedMajor: Int,
    /**
     * 后端实际报告的 LLVM 版本字符串。
     */
    val actualVersion: String,
) : LlvmBackendException("LLVM backend version mismatch: expected major $expectedMajor, actual '$actualVersion'")

/**
 * LLVM JNI 后端缺少必需原生符号时的异常。
 */
class LlvmBackendMissingSymbolsException(
    /**
     * 缺失的原生符号名集合。
     */
    val symbols: Set<String>,
) : LlvmBackendException("LLVM backend missing required symbols: ${symbols.joinToString(", ")}")
