package org.cangnova.cangjie.llvm.api

/**
 * LLVM 互操作层异常基类。
 */
open class LlvmException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * 模块或函数校验失败时抛出的异常。
 */
class LlvmVerificationException(
    message: String,
) : LlvmException(message)

/**
 * LLVM 后端不可用时抛出的异常。
 */
class LlvmBackendUnavailableException(
    message: String,
    cause: Throwable? = null,
) : LlvmException(message, cause)

/**
 * LLVM 主版本不匹配时抛出的异常。
 */
class LlvmVersionMismatchException(
    val expectedMajor: Int,
    val actualVersion: String,
) : LlvmException("LLVM major version mismatch: expected $expectedMajor but got $actualVersion")
