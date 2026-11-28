package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.parsing.CjMemberParsingContext
import org.cangnova.cangjie.parsing.CjParameterGrammar
import java.util.Collections

/**
 * raw surface 采集时的实际词法 owner 快照；不保存可变 builder Context，也不通过名字认领符号。
 * symbol/target 可以在采集时尚未绑定；宿主 raw 构造完成后消费同一对象身份。
 * 文件身份由 construction 的 PreMacroCfirFile 提供，顶层容器路径合法为空。
 */
class MacroRawOwnerSnapshot(
    val packageFqName: FqName,
    containerSymbols: List<CfirBasedSymbol<*>>,
    val dispatchReceiverType: ConeSimpleCangJieType?,
    val inLocalContext: Boolean,
    val functionTarget: CfirFunctionTarget?,
    val signature: MacroParameterSignature?,
    /** 词法名义 owner 的原始名字；是否使用成员文法由实际替换槽位决定。 */
    val nominalSyntax: CjMemberParsingContext?,
    capturedTypeParameters: List<MacroRawTypeParameterScope>,
    constructedClasses: List<MacroRawConstructedClassScope>,
) {
    val containerSymbols: List<CfirBasedSymbol<*>> = Collections.unmodifiableList(containerSymbols.toList())
    /** 宏新 token 恢复宿主捕获链，不能用私有 header 或名称重新认领类型参数。 */
    val capturedTypeParameters: List<MacroRawTypeParameterScope> =
        Collections.unmodifiableList(capturedTypeParameters.toList())
    /** 宏生成的构造器/enum 构造项须引用宿主名义类型的参数，随真实 owner 帧一并恢复。 */
    val constructedClasses: List<MacroRawConstructedClassScope> =
        Collections.unmodifiableList(constructedClasses.toList())
}

/** 可构造名义类型（class/struct/enum）的不可变帧：实际容器 symbol 与其 header 参数 symbol。 */
class MacroRawConstructedClassScope(
    val owner: CfirBasedSymbol<*>,
    symbols: List<CfirTypeParameterSymbol>,
) {
    val symbols: List<CfirTypeParameterSymbol> = Collections.unmodifiableList(symbols.toList())
}

/** 一个不可变词法捕获层；不持有 raw builder，也不读取尚未绑定的声明。 */
class MacroRawTypeParameterScope(
    val isLocal: Boolean,
    symbols: List<CfirTypeParameterSymbol>,
) {
    val symbols: List<CfirTypeParameterSymbol> = Collections.unmodifiableList(symbols.toList())
}

/**
 * 参数及其默认值的实际词法签名；进入内层函数/lambda body 后必须屏蔽外层签名。
 * 它不是私有副本参数的结构 owner，后者由 raw 参数构造入口显式提供。
 */
sealed interface MacroParameterSignature {
    val owner: CfirBasedSymbol<*>

    data class Declaration(
        override val owner: CfirBasedSymbol<*>,
        val grammar: CjParameterGrammar,
    ) : MacroParameterSignature

    /** Lambda 使用独立参数文法，不能伪装成普通声明参数宏入口。 */
    data class Lambda(override val owner: CfirBasedSymbol<*>) : MacroParameterSignature
}
