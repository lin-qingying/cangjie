package org.cangnova.cangjie.serialization.deserialization

/**
 * 对齐 Kotlin `IncompatibleVersionErrorData`。
 */
data class IncompatibleVersionErrorData<out T>(
    val actualVersion: T,
    val compilerVersion: T,
    val languageVersion: T,
    val expectedVersion: T,
    val filePath: String,
)
