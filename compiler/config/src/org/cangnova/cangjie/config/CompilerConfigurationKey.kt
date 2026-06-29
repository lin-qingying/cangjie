package org.cangnova.cangjie.config

import com.intellij.openapi.util.Key

/**
 * 编译配置键。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.config.CompilerConfigurationKey`。
 */
class CompilerConfigurationKey<out T : Any> private constructor(
    /**
     * IntelliJ 平台层用于实际索引配置值的 key。
     */
    internal val ideaKey: Key<@UnsafeVariance T>,
) {
    /** 使用键名创建配置键。 */
    constructor(name: String) : this(Key.create(name))

    /**
     * 返回底层 IntelliJ key 的调试文本。
     */
    override fun toString(): String = ideaKey.toString()

    companion object {
        /** 静态工厂。 */
        @JvmStatic
        fun <T : Any> create(name: String): CompilerConfigurationKey<T> = CompilerConfigurationKey(name)
    }
}
