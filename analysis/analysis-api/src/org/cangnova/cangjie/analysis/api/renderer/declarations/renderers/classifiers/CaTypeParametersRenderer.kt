package org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol

/**
 * 类型形参列表 renderer。
 *
 * 决定 `<T1, T2 where T1 <: B>` 整体形态; 是否输出 where 子句、上界放在尖括号内还是放在 where
 * 中, 都由具体实现决定。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeParametersRenderer`。
 */
fun interface CaTypeParametersRenderer {
    /** 写出 [owner] 的全部类型形参列表到 [printer]。 */
    fun renderTypeParameters(
        analysisSession: CaSession,
        owner: CaTypeParameterOwnerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )
}
