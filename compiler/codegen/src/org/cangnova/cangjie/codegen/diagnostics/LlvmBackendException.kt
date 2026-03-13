package org.cangnova.cangjie.codegen.diagnostics

open class LlvmBackendException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class LlvmBackendUnavailableException(
    message: String,
    cause: Throwable? = null,
) : LlvmBackendException(message, cause)

class LlvmBackendVersionMismatchException(
    val expectedMajor: Int,
    val actualVersion: String,
) : LlvmBackendException("LLVM backend version mismatch: expected major $expectedMajor, actual '$actualVersion'")

class LlvmBackendMissingSymbolsException(
    val symbols: Set<String>,
) : LlvmBackendException("LLVM backend missing required symbols: ${symbols.joinToString(", ")}")

