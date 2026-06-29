package org.cangnova.cangjie.cfir.diagnostics

import com.google.common.collect.ImmutableSet
import com.intellij.util.containers.ContainerUtil

/**
 * 仓颉诊断 suppress 查询的抽象弱缓存。
 *
 * 具体实现负责从语言结构中找到带 suppress 标记的祖先节点，并提供 suppress 字符串集合。
 */
abstract class AbstractCangJieSuppressCache<Element> {
    // The cache is weak: we're OK with losing it
    /**
     * 从已注解元素到 suppressor 的弱值并发缓存。
     */
    protected val suppressors = ContainerUtil.createConcurrentWeakValueMap<Element, Suppressor<Element>>()

    /**
     * 判断指定诊断 key 和严重级别是否被当前元素或祖先元素 suppress。
     */
    fun isSuppressed(element: Element, rootElement: Element, suppressionKey: String, severity: Severity) =
        isSuppressed(StringSuppressRequest(element, rootElement, severity, suppressionKey.lowercase()))

    /**
     * 处理一次 suppress 查询请求。
     */
    protected open fun isSuppressed(request: SuppressRequest<Element>): Boolean {

        val annotated = getClosestAnnotatedAncestorElement(request.element, request.rootElement, false) ?: return false

        return isSuppressedByAnnotated(request.suppressKey, request.severity, annotated, request.rootElement, 0)

    }

    /**
     * 查找离指定元素最近的带 suppress 注解祖先。
     */
    protected abstract fun getClosestAnnotatedAncestorElement(element: Element, rootElement: Element, excludeSelf: Boolean): Element?

    /*
       The cache is optimized for the case where no warnings are suppressed (most frequent one)

       trait Root {
         suppress("X")
         trait A {
           trait B {
             suppress("Y")
             trait C {
               fun foo() = warning
             }
           }
         }
       }

       Nothing is suppressed at foo, so we look above. While looking above we went up to the root (once) and propagated
       all the suppressors down, so now we have:

          foo  - suppress(Y) from C
          C    - suppress(Y) from C
          B    - suppress(X) from A
          A    - suppress(X) from A
          Root - suppress() from Root

       Next time we look up anything under foo, we try the Y-suppressor and then immediately the X-suppressor, then to the empty
       suppressor at the root. All the intermediate empty nodes are skipped, because every suppressor remembers its definition point.

       This way we need no more lookups than the number of suppress() annotations from here to the root.
     */
    /**
     * 从已注解节点向上递归查询 suppress，并在可支配时把上层 suppressor 缓存下推。
     */
    protected open fun isSuppressedByAnnotated(
        suppressionKey: String,
        severity: Severity,
        annotated: Element,
        rootElement: Element,
        debugDepth: Int
    ): Boolean {
        val suppressor = getOrCreateSuppressor(annotated)
        if (suppressor.isSuppressed(suppressionKey, severity)) return true

        val annotatedAbove = getClosestAnnotatedAncestorElement(suppressor.annotatedElement, rootElement, true) ?: return false

        val suppressed = isSuppressedByAnnotated(suppressionKey, severity, annotatedAbove, rootElement, debugDepth + 1)
        val suppressorAbove = suppressors[annotatedAbove]
        if (suppressorAbove != null && suppressorAbove.dominates(suppressor)) {
            suppressors[annotated] = suppressorAbove
        }

        return suppressed
    }

    /**
     * 获取或创建指定注解节点对应的 suppressor。
     */
    protected fun getOrCreateSuppressor(annotated: Element): Suppressor<Element> =
        suppressors.getOrPut(annotated) {
            val strings = getSuppressingStrings(annotated)
            when (strings.size) {
                0 -> EmptySuppressor(annotated)
                1 -> SingularSuppressor(annotated, strings.first())
                else -> MultiSuppressor(annotated, strings)
            }
        }

    // TODO: consider replacing set with list, assuming that the list of suppresses is usually very small
    /**
     * 从已注解元素上读取 suppress 字符串集合。
     */
    protected abstract fun getSuppressingStrings(annotated: Element): Set<String>

    companion object {
        /**
         * 按 suppress 字符串集合判断指定诊断 key 是否被屏蔽。
         */
        private fun isSuppressedByStrings(key: String, strings: Set<String>, severity: Severity): Boolean =
            severity == Severity.WARNING && "warnings" in strings || key.lowercase() in strings
    }

    /**
     * 单个已注解元素上的 suppress 判断器。
     */
    protected abstract class Suppressor<Element>(
        /**
         * 定义该 suppressor 的已注解元素。
         */
        val annotatedElement: Element,
    ) {
        /**
         * 判断指定诊断 key 和严重级别是否被该 suppressor 屏蔽。
         */
        abstract fun isSuppressed(suppressionKey: String, severity: Severity): Boolean

        // true is \forall x. other.isSuppressed(x) -> this.isSuppressed(x)
        /**
         * 判断当前 suppressor 是否覆盖另一个 suppressor 的全部屏蔽能力。
         */
        abstract fun dominates(other: Suppressor<Element>): Boolean
    }

    /**
     * 不屏蔽任何诊断的 suppressor。
     */
    private class EmptySuppressor<Element>(annotated: Element) : Suppressor<Element>(annotated) {
        /**
         * 空 suppressor 永远不屏蔽诊断。
         */
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean = false
        /**
         * 空 suppressor 只支配另一个空 suppressor。
         */
        override fun dominates(other: Suppressor<Element>): Boolean = other is EmptySuppressor
    }

    /**
     * 只包含一个 suppress 字符串的 suppressor。
     */
    private class SingularSuppressor<Element>(annotated: Element, private val string: String) : Suppressor<Element>(annotated) {
        /**
         * 根据单个 suppress 字符串判断诊断是否被屏蔽。
         */
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean {
            return isSuppressedByStrings(suppressionKey, ImmutableSet.of(string), severity)
        }

        /**
         * 单值 suppressor 支配空 suppressor 或相同字符串的单值 suppressor。
         */
        override fun dominates(other: Suppressor<Element>): Boolean {
            return other is EmptySuppressor || (other is SingularSuppressor && other.string == string)
        }
    }

    /**
     * 包含多个 suppress 字符串的 suppressor。
     */
    private class MultiSuppressor<Element>(annotated: Element, private val strings: Set<String>) : Suppressor<Element>(annotated) {
        /**
         * 根据 suppress 字符串集合判断诊断是否被屏蔽。
         */
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean {
            return isSuppressedByStrings(suppressionKey, strings, severity)
        }

        /**
         * 多值 suppressor 只保守地支配空 suppressor，避免做昂贵集合包含判断。
         */
        override fun dominates(other: Suppressor<Element>): Boolean {
            // it's too costly to check set inclusion
            return other is EmptySuppressor
        }
    }

    /**
     * 一次 suppress 查询所需的输入数据。
     */
    protected interface SuppressRequest<Element> {
        /**
         * 触发诊断的元素。
         */
        val element: Element
        /**
         * 查询允许上溯的根元素。
         */
        val rootElement: Element
        /**
         * 被查询诊断的严重级别。
         */
        val severity: Severity
        /**
         * 被查询诊断的标准化 suppress key。
         */
        val suppressKey: String
    }

    /**
     * 基于诊断字符串 key 的 suppress 查询请求。
     */
    private class StringSuppressRequest<Element>(
        /**
         * 触发诊断的元素。
         */
        override val element: Element,
        /**
         * 查询允许上溯的根元素。
         */
        override val rootElement: Element,
        /**
         * 被查询诊断的严重级别。
         */
        override val severity: Severity,
        /**
         * 被查询诊断的标准化 suppress key。
         */
        override val suppressKey: String
    ) : SuppressRequest<Element>
}

