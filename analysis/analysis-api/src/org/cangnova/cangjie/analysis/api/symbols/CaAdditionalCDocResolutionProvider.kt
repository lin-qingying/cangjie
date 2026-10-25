package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjElement

/**
 * 为 CDoc 链接解析提供额外符号的扩展点。
 *
 * 主解析器只在作用域、类型限定成员和包查找均未命中时查询此扩展点；实现应返回
 * 与给定全限定名及 CDoc 上下文相符的符号，不改变常规名称解析优先级。
 * 对齐 Kotlin Analysis API `KaAdditionalKDocResolutionProvider`。
 */
@CaExperimentalApi
interface CaAdditionalCDocResolutionProvider {
    /**
     * 为 [contextElement] 中的 CDoc 全限定链接提供附加解析结果。
     */
    fun resolveCDocFqName(
        analysisSession: CaSession,
        fqName: FqName,
        contextElement: CjElement,
    ): Collection<CaSymbol>
}
