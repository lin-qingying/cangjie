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
 */
object CaAnnotationRendererForSource {
    val WITH_QUALIFIED_NAMES: CaAnnotationRenderer = CaAnnotationRenderer {
        annotationListRenderer = CaAnnotationListRenderer.FOR_SOURCE
        annotationQualifierRenderer = CaAnnotationQualifierRenderer.WITH_QUALIFIED_NAMES
        annotationArgumentsRenderer = CaAnnotationArgumentsRenderer.IF_ANY
    }

    val WITH_SHORT_NAMES: CaAnnotationRenderer = WITH_QUALIFIED_NAMES.with {
        annotationQualifierRenderer = CaAnnotationQualifierRenderer.WITH_SHORT_NAMES
    }
}
