package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * renderer 代码风格协议。
 *
 * 当前先稳定控制最常用的空白策略，后续如果仓颉源码风格需要继续细化，
 * 可以在这一层继续扩展。
 */
interface CaRendererCodeStyle {
    public fun getIndentSize(analysisSession: CaSession): Int

    public fun getSeparatorAfterContextReceivers(analysisSession: CaSession): String

    public fun getSeparatorBetweenAnnotationAndOwner(analysisSession: CaSession, symbol: CaAnnotated): String

    public fun getSeparatorBetweenAnnotations(analysisSession: CaSession, symbol: CaAnnotated): String

    public fun getSeparatorBetweenModifiers(analysisSession: CaSession): String

    public fun getSeparatorBetweenMembers(
        analysisSession: CaSession,
        first: CaDeclarationSymbol,
        second:CaDeclarationSymbol,
    ): String
}
