package org.cangnova.cangjie.llvm.api

open class LlvmException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class LlvmVerificationException(
    message: String,
) : LlvmException(message)

class LlvmBackendUnavailableException(
    message: String,
    cause: Throwable? = null,
) : LlvmException(message, cause)

class LlvmVersionMismatchException(
    val expectedMajor: Int,
    val actualVersion: String,
) : LlvmException("LLVM major version mismatch: expected $expectedMajor but got $actualVersion")
