package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType


/**
 * 推荐使用的默认代码风格预设。
 *
 * - 缩进 4 空格(对齐仓颉社区常用风格);
 * - 类型/类型形参/形参上的注解使用空格分隔, 其余使用换行;
 * - 修饰符之间使用单空格;
 * - enum 构造子之间使用 `,\n`, 与其他成员之间使用 `;\n\n`, 其余成员之间使用空行分隔。
 *
 * 对齐 Kotlin Analysis API 的 `KaRecommendedRendererCodeStyle`, 标注为实验性 API 是因为
 * 后续可能继续微调具体策略。
 */
@CaExperimentalApi
object CaRecommendedRendererCodeStyle : CaRendererCodeStyle {
    /** 默认缩进 4 个空格。 */
    override fun getIndentSize(analysisSession: CaSession): Int = 4

    /** 上下文 receiver 后换行, 让后续声明独立成行。 */
    override fun getSeparatorAfterContextReceivers(analysisSession: CaSession): String = "\n"

    /**
     * 注解与其所属元素之间的分隔。
     *
     * 用于类型/类型形参/参数等"内联位置"时使用空格,
     * 其余顶层声明使用换行以贴近源码排版。
     */
    override fun getSeparatorBetweenAnnotationAndOwner(analysisSession: CaSession, symbol: CaAnnotated): String = when (symbol) {
        is CaType -> " "
        is CaTypeParameterSymbol -> " "
        is CaParameterSymbol -> " "
        else -> "\n"
    }

    /** 注解之间的分隔, 规则与 [getSeparatorBetweenAnnotationAndOwner] 一致。 */
    override fun getSeparatorBetweenAnnotations(analysisSession: CaSession, symbol: CaAnnotated): String = when (symbol) {
        is CaType -> " "
        is CaTypeParameterSymbol -> " "
        is CaParameterSymbol -> " "
        else -> "\n"
    }

    /** 修饰符之间统一使用单空格。 */
    override fun getSeparatorBetweenModifiers(analysisSession: CaSession): String = " "

    /**
     * 成员之间的分隔。
     *
     * - 连续两个 enum 构造子: 使用 `,\n`;
     * - 最后一个 enum 构造子接续其他成员: 使用 `;\n\n`;
     * - 其余成员之间: 使用空行 `\n\n`。
     */
    override fun getSeparatorBetweenMembers(analysisSession: CaSession, first: CaDeclarationSymbol, second: CaDeclarationSymbol): String {
        return when {
            first is CaEnumConstructorSymbol && second is CaEnumConstructorSymbol -> ",\n"
            first is CaEnumConstructorSymbol && second !is CaEnumConstructorSymbol -> ";\n\n"
            else -> "\n\n"
        }
    }


}
