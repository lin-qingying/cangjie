package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 单个超类型 renderer。
 *
 * 决定列表中每个超类型如何写出, 例如是否经过类型近似化, 是否使用全限定名等。
 *
 * 对齐 Kotlin Analysis API 的 `KaSuperTypeRenderer`。
 */
fun interface CaSuperTypeRenderer {
    /** 把单个 [superType] 写入 [printer]。 */
    fun renderSuperType(
        analysisSession: CaSession,
        superType: CaType,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
