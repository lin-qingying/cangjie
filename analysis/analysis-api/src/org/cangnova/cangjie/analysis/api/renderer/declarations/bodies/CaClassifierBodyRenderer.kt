package org.cangnova.cangjie.analysis.api.renderer.declarations.bodies

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrintWithSettingsFrom
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import kotlin.text.append

/**
 * classifier(类、接口、struct、enum)主体 renderer。
 *
 * 决定主体大括号是否输出、内部成员如何排列。具体成员渲染交由
 * [CaDeclarationRenderer.bodyMemberScopeProvider] / [CaDeclarationRenderer.bodyMemberScopeSorter]
 * 与各 kind 子 renderer 处理。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassifierBodyRenderer`。
 */
fun interface CaClassifierBodyRenderer {
    /** 渲染 [symbol] 的主体到 [printer]。 */
    fun renderBody(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    companion object {
        /** 预设: 渲染成员; 即便成员为空也输出 `{ }`。 */
        val BODY_WITH_MEMBERS_OR_EMPTY_BRACES = CaClassifierBodyWithMembersRenderer {
            true

        }
        /** 预设: 不输出主体, 仅渲染头部声明。 */
        val NO_BODY = CaClassifierBodyRenderer { analysisSession,
                                                 symbol,
                                                 declarationRenderer,
                                                 printer ->
        }
        /** 预设: 渲染成员, 但成员为空时整个主体被省略。 */
        val BODY_WITH_MEMBERS = CaClassifierBodyWithMembersRenderer {
            false
        }
    }


}

/**
 * 真正负责"枚举成员并按代码风格排版"的 renderer。
 *
 * 子类只需通过 [renderEmptyBodyForEmptyMemberScope] 决定空成员是否仍输出大括号。
 *
 * 对齐 Kotlin Analysis API 的 `KaClassifierBodyWithMembersRenderer`。
 */
fun interface CaClassifierBodyWithMembersRenderer : CaClassifierBodyRenderer {
    /** 当成员作用域为空时, 是否仍输出空大括号 `{ }`。 */
    fun renderEmptyBodyForEmptyMemberScope(symbol: CaDeclarationContainerSymbol): Boolean

    /**
     * 默认实现:
     * - 通过 provider 拿到成员, 排除主构造器(已在头部渲染);
     * - 经 sorter 排序后逐个渲染;
     * - 成员之间使用 [CaRendererCodeStyle.getSeparatorBetweenMembers] 提供的分隔串;
     * - 当成员为空且 [renderEmptyBodyForEmptyMemberScope] 返回 false 时直接跳过。
     */
    override fun renderBody(
        analysisSession: CaSession,
        symbol: CaDeclarationContainerSymbol,
        declarationRenderer: CaDeclarationRenderer,
        printer: PrettyPrinter,
    ) {
        val members = declarationRenderer.bodyMemberScopeProvider.getMemberScope(analysisSession, symbol)
            .filter { it !is CaConstructorSymbol || !it.isPrimary }
            .let { declarationRenderer.bodyMemberScopeSorter.sortMembers(analysisSession, it, symbol) }

        val membersToPrint = members.mapNotNull { member ->
            val rendered = prettyPrintWithSettingsFrom(printer) {
                declarationRenderer.renderDeclaration(analysisSession, member, this)
            }
            if (rendered.isNotEmpty()) member to rendered else null
        }

        if (membersToPrint.isEmpty() && !renderEmptyBodyForEmptyMemberScope(symbol)) return

        printer.withIndentInBraces {
            var previous: CaDeclarationSymbol? = null
            for ((member, rendered) in membersToPrint) {
                if (previous != null) {
                    printer.append(
                        declarationRenderer.codeStyle.getSeparatorBetweenMembers(
                            analysisSession,
                            previous,
                            member
                        )
                    )
                }
                previous = member
                printer.append(rendered)
            }
        }
    }
}
