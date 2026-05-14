package org.cangnova.cangjie.analysis.api.renderer.base.annotations

import org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers.CaAnnotationArgumentsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers.CaAnnotationListRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.renderers.CaAnnotationQualifierRenderer

/**
 * 面向源码展示的注解 renderer 预设。
 *
 * 约束：
 * - 内置注解始终保持仓颉语言级短名，不受 qualified preset 影响。
 * - 只有自定义注解会在短名 / 限定名之间切换。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotationRendererForSource`。
 */
object CaAnnotationRendererForSource {
    /**
     * 预设: 自定义注解使用全限定名(`org.foo.Bar`), 内置注解仍用短名。
     *
     * 适合 IDE/debug 等需要消歧义的场景。
     */
    val WITH_QUALIFIED_NAMES: CaAnnotationRenderer = CaAnnotationRenderer {
        annotationListRenderer = CaAnnotationListRenderer.FOR_SOURCE
        annotationQualifierRenderer = CaAnnotationQualifierRenderer.WITH_QUALIFIED_NAMES
        annotationArgumentsRenderer = CaAnnotationArgumentsRenderer.IF_ANY
    }

    /**
     * 预设: 全部注解都使用短名(`Bar`)。
     *
     * 适合贴近源码风格的展示, 假设上下文有合适的 import。
     */
    val WITH_SHORT_NAMES: CaAnnotationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationQualifierRenderer = CaAnnotationQualifierRenderer.WITH_SHORT_NAMES
    }
}
