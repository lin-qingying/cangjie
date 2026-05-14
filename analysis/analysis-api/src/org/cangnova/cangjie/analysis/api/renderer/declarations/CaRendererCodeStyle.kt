package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol

/**
 * renderer 代码风格协议。
 *
 * 当前先稳定控制最常用的空白策略，后续如果仓颉源码风格需要继续细化，
 * 可以在这一层继续扩展。
 *
 * 对齐 Kotlin Analysis API 的 `KaRendererCodeStyle`。
 */
interface CaRendererCodeStyle {
    /** 单层缩进使用的空格数。 */
    fun getIndentSize(analysisSession: CaSession): Int

    /** 上下文 receiver 渲染完后跟随的分隔串(通常为换行)。 */
    fun getSeparatorAfterContextReceivers(analysisSession: CaSession): String

    /** 注解列表与其归属声明/类型之间的分隔(空格或换行)。 */
    fun getSeparatorBetweenAnnotationAndOwner(analysisSession: CaSession, symbol: CaAnnotated): String

    /** 多个注解之间的分隔(空格或换行)。 */
    fun getSeparatorBetweenAnnotations(analysisSession: CaSession, symbol: CaAnnotated): String

    /** 多个修饰符之间的分隔(通常为空格)。 */
    fun getSeparatorBetweenModifiers(analysisSession: CaSession): String

    /**
     * 类/接口主体中两个相邻成员之间的分隔。
     *
     * 实现可基于 [first] / [second] 的 kind 给出更精细的策略,
     * 例如 enum 构造子之间使用 `,\n`、构造子与其他成员之间使用 `;\n\n`。
     */
    fun getSeparatorBetweenMembers(
        analysisSession: CaSession,
        first: CaDeclarationSymbol,
        second:CaDeclarationSymbol,
    ): String
}
