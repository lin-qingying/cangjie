package org.cangnova.cangjie.analysis.api.renderer.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.render

/**
 * 注解参数常量值的字符串渲染器。
 *
 * - 按仓颉语法渲染公开模型能稳定表示的常量、枚举、tuple 和数组值;
 * - class/struct 实例仅在保留了源码构造调用 PSI 时原样渲染; 不把求值后字段伪装成构造器参数。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationValueRenderer`。
 */
object CaAnnotationValueRenderer {

    /** 将 [value] 渲染为字符串形式, 便于在诊断或 IDE 提示中显示。 */
    fun render(value: CaAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    /** 按注解值的语义种类渲染仓颉源码形式。 */
    private fun StringBuilder.renderConstantValue(value: CaAnnotationValue) {
        when (value) {
            is CaAnnotationValue.ConstantValue -> append(value.value.render())

            is CaAnnotationValue.TupleValue -> append(
                value.values.joinToString(", ", prefix = "(", postfix = ")") {
                    CaAnnotationValueRenderer.render(it)
                },
            )

            is CaAnnotationValue.ArrayValue -> append(
                value.values.joinToString(", ", prefix = "[", postfix = "]") {
                    CaAnnotationValueRenderer.render(it)
                },
            )

            is CaAnnotationValue.EnumValue -> {
                val callableId = value.callableId ?: return
                append(callableId.asSingleFqName().render())
                if (value.arguments.isNotEmpty()) {
                    append(value.arguments.joinToString(", ", prefix = "(", postfix = ")") {
                        CaAnnotationValueRenderer.render(it)
                    })
                }
            }

            is CaAnnotationValue.ClassInstanceValue,
            is CaAnnotationValue.StructInstanceValue,
            -> append((value.sourcePsi as? CjCallExpression)?.text.orEmpty())
        }
    }
}
