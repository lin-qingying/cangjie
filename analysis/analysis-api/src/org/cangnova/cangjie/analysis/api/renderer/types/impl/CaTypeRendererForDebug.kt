package org.cangnova.cangjie.analysis.api.renderer.types.impl

import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaClassTypeQualifierRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaErrorTypeRenderer

/**
 * 面向 debug 输出的类型 renderer 预设集合。
 *
 * - 在 [CaTypeRendererForSource] 各预设基础上, 把 error type 切换为带错误消息的形态,
 *   便于排查未解析/语义错误的类型;
 * - 命名约定与 source 预设一一对应。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeRendererForDebug`。
 */
object CaTypeRendererForDebug {
    /** debug 基线: 全限定名 + 输出 error type 的错误信息。 */
    val WITH_QUALIFIED_NAMES: CaTypeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
        errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
    }

    /** debug 短名版本。 */
    val WITH_SHORT_NAMES: CaTypeRenderer = CaTypeRendererForSource.WITH_SHORT_NAMES.with {
        classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
        errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
    }



    /** debug 短名 + 隐藏类型实参版本。 */
    val WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS: CaTypeRenderer =
        CaTypeRendererForSource.WITH_SHORT_NAMES_WITHOUT_TYPE_ARGUMENTS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
            errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
        }

    /** debug 全限定名 + 隐藏函数 kind 关键字版本。 */
    val WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer =
        CaTypeRendererForSource.WITH_QUALIFIED_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_QUALIFIED_NAMES
            errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
        }

    /** debug 短名 + 隐藏函数 kind 关键字版本。 */
    val WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS: CaTypeRenderer =
        CaTypeRendererForSource.WITH_SHORT_NAMES_WITHOUT_FUNCTION_KIND_KEYWORDS.with {
            classIdRenderer = CaClassTypeQualifierRenderer.WITH_SHORT_NAMES
            errorTypeRenderer = CaErrorTypeRenderer.WITH_ERROR_MESSAGE
        }

}
