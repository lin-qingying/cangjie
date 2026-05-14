package org.cangnova.cangjie.analysis.api.renderer.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue

/**
 * 注解参数常量值的字符串渲染器。
 *
 * - 把 [CaAnnotationValue] 序列化为可在源码/调试视图中显示的字面量;
 * - 当前实现是占位骨架, 后续随着仓颉注解常量模型扩展再补齐。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationValueRenderer`。
 */
object CaAnnotationValueRenderer {

    /** 将 [value] 渲染为字符串形式, 便于在诊断或 IDE 提示中显示。 */
    fun render(value: CaAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    /** 实际写入逻辑(内部使用), 当前为占位实现。 */
    private fun StringBuilder.renderConstantValue(value: CaAnnotationValue) {

    }
}