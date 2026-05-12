package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression

interface MacroCallInfoFactory {
    fun create(file: CfirFile, expression: CfirMacroExpression): MacroCallInfo
}

/**
 * 旧 single-token semantic 路径实现。
 *
 * 仅 Batch 10 过渡期保留；新代码应当通过
 * `org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface.attrTokens`
 * / `inputTokens` 直接消费真实 token 流
 * （baseline 第 2 节硬性边界 #8："single-token input ... 只可用于 debug/display"）。
 *
 * 生产代码不得继续使用该实现；保留源码仅用于旧路径审计。
 */
@Deprecated(
    message = "Use MacroSurface.attrTokens / inputTokens from macro construction step " +
        "instead of single-token MacroCallInfo (baseline 第 2 节硬性边界 #8).",
    level = DeprecationLevel.ERROR,
)
class DefaultMacroCallInfoFactory : MacroCallInfoFactory {
    override fun create(file: CfirFile, expression: CfirMacroExpression): MacroCallInfo {
        val source = expression.source
        val linesMapping = file.sourceFileLinesMapping
        val startPosition = source.toSourcePosition(linesMapping)
        val endPosition = source.toEndSourcePosition(linesMapping)
        val macroName = expression.name?.asString().orEmpty()

        return MacroCallInfo(
            idName = macroName,
            methodName = macroName,
            packageName = file.packageDirective.packageFqName.asString().takeUnless { it == "<root>" }.orEmpty(),
            hasAttrs = !expression.attrText.isNullOrBlank(),
            argTokens = expression.inputText.asSingleTokenList(startPosition, endPosition),
            attrTokens = expression.attrText.asSingleTokenList(startPosition, endPosition),
            position = startPosition,
            endPosition = endPosition,
        )
    }
}
