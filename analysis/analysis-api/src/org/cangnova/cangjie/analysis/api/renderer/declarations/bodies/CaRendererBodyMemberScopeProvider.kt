package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol

/**
 * 类主体成员作用域提供者。
 *
 * 决定从给定的 container symbol 取出哪些成员参与主体渲染。
 * 不同 preset 决定是否包含合成/继承成员等。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererBodyMemberScopeProvider`。
 */
fun interface CaRendererBodyMemberScopeProvider {

    /** 返回 [symbol] 的成员列表(顺序不限, 后续会经 sorter 排序)。 */
    fun getMemberScope(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol
    ): List<CaDeclarationSymbol>

    companion object {
        /** 预设: 返回 combined declared 成员作用域中的全部成员。 */
        val ALL: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
                with(analysisSession) {
                    symbol.combinedDeclaredMemberScope.declarations.toList()
                }
            }

        /**
         * 预设: 仅保留真正源代码声明的成员, 过滤掉 SUBSTITUTION_OVERRIDE / INTERSECTION_OVERRIDE 等
         * 合成 override 成员, 避免重复展示。
         */
        val ALL_DECLARED: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { analysisSession, symbol ->
                with(analysisSession) {
                    symbol.combinedDeclaredMemberScope.declarations
                        .filter { member ->
                            val origin = member.origin
                            origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE_DECLARATION_SITE &&
                                    origin != CaSymbolOrigin.SUBSTITUTION_OVERRIDE_CALL_SITE &&
                                    origin != CaSymbolOrigin.INTERSECTION_OVERRIDE
                        }
                        .toList()
                }
            }

        /** 预设: 返回空列表, 等同于"主体不渲染任何成员"。 */
        val NONE: CaRendererBodyMemberScopeProvider =
            CaRendererBodyMemberScopeProvider { _, _ -> emptyList() }
    }
}
