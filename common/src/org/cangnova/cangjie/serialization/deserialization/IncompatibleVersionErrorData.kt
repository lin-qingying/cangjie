package org.cangnova.cangjie.serialization.deserialization

/**
 * 对齐 Kotlin `IncompatibleVersionErrorData`。
 */
data class IncompatibleVersionErrorData<out T>(
    /**
     * 二进制载体中实际记录的版本。
     */
    val actualVersion: T,
    /**
     * 当前编译器自身的版本。
     */
    val compilerVersion: T,
    /**
     * 当前编译配置使用的语言版本。
     */
    val languageVersion: T,
    /**
     * 当前反序列化逻辑期望能够读取的版本。
     */
    val expectedVersion: T,
    /**
     * 产生版本不兼容问题的二进制文件路径。
     */
    val filePath: String,
)
