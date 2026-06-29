package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.cfir.diagnostics.DiagnosticBaseContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext

/**
 * 标记不携带 DiagnosticBaseContext 的旧渲染上下文 API。
 */
@RequiresOptIn("Legacy API for K1 that doesn't pass a DiagnosticBaseContext. Mustn't be used with K2.")
annotation class LegacyRenderingContextApi

// holds data about the parameters of the diagnostic we're about to render
/**
 * 诊断参数渲染过程中的上下文和惰性计算缓存。
 */
sealed class RenderingContext {
    /**
     * 读取指定渲染上下文 key 的值。
     */
    abstract operator fun <T> get(key: Key<T>): T

    /**
     * 渲染上下文中可惰性计算的键。
     */
    abstract class Key<out T>(
        /**
         * key 的调试名称。
         */
        val name: String,
    ) {
        /**
         * 根据待渲染对象和诊断上下文计算 key 值。
         */
        abstract fun compute(objectsToRender: Collection<Any?>, diagnosticContext: DiagnosticBaseContext): T
    }

    /**
     * 携带待渲染对象集合的渲染上下文实现。
     */
    class Impl(
        /**
         * 当前诊断消息中的参数对象集合。
         */
        private val objectsToRender: Collection<Any?>,
        /**
         * 当前诊断所属的基础上下文。
         */
        private val diagnosticContext: DiagnosticBaseContext,
    ) : RenderingContext() {
        @LegacyRenderingContextApi
        constructor(objectsToRender: Collection<Any?>) : this(objectsToRender, DiagnosticContext.Default)

        /**
         * 已计算 key 值缓存。
         */
        private val data = linkedMapOf<Key<*>, Any?>()

        /**
         * 返回 key 对应值，缺失时计算并缓存。
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T> get(key: Key<T>): T {
            return data[key] as? T ?: key.compute(objectsToRender, diagnosticContext).also { data[key] = it }
        }
    }

    /**
     * 没有待渲染对象的空上下文。
     */
    object Empty : RenderingContext() {
        /**
         * 使用默认诊断上下文计算 key 值。
         */
        override fun <T> get(key: Key<T>): T {
            return key.compute(emptyList(), DiagnosticContext.Default)
        }
    }

    companion object {
        /**
         * 使用旧 API 创建渲染上下文。
         */
        @JvmStatic
        @LegacyRenderingContextApi
        fun of(vararg objectsToRender: Any?): RenderingContext {
            return Impl(objectsToRender.toList())
        }

        /**
         * 使用显式诊断上下文创建渲染上下文。
         */
        fun of(diagnosticContext: DiagnosticBaseContext, vararg objectsToRender: Any?): RenderingContext {
            return Impl(objectsToRender.toList(), diagnosticContext)
        }
    }
}

