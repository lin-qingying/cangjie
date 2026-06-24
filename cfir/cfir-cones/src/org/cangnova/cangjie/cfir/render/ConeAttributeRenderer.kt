package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeAttribute
import org.cangnova.cangjie.utils.ifNotEmpty

/**
 * Cone 类型属性的渲染策略。
 *
 * 类型属性既可能服务调试输出，也可能服务用户可读的诊断文本。该抽象把属性遍历
 * 与具体输出策略分离，使 [ConeTypeRenderer] 可以按场景切换属性格式。
 */
abstract class ConeAttributeRenderer {
    /**
     * 将 [attributes] 渲染为追加在类型文本前方的字符串。
     */
    abstract fun render(attributes: Iterable<ConeAttribute<*>>): String

    /**
     * 直接使用属性自身 [toString] 的调试渲染策略。
     */
    object ToString : ConeAttributeRenderer() {
        /**
         * 按属性键稳定排序并输出全部属性。
         */
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return attributes.sortedBy { it.key.qualifiedName }.joinToString(separator = " ", postfix = " ")
        }
    }

    /**
     * 面向用户可读文本的属性渲染策略。
     */
    object ForReadability : ConeAttributeRenderer() {
        /**
         * 只输出声明了可读文本的属性，并按属性键稳定排序。
         */
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return attributes.mapNotNull { attribute -> attribute.renderForReadability()?.let { attribute to it } }
                .sortedBy { (attribute, _) -> attribute.key.qualifiedName }
                .ifNotEmpty {
                    joinToString(separator = " ", postfix = " ") { (_, output) ->
                        output
                    }
                } ?: ""
        }
    }

    /**
     * 完全抑制属性输出的渲染策略。
     */
    object None : ConeAttributeRenderer() {
        /**
         * 始终返回空字符串。
         */
        override fun render(attributes: Iterable<ConeAttribute<*>>): String {
            return ""
        }
    }
}
