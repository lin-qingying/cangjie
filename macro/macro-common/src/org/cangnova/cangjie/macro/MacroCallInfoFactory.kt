package org.cangnova.cangjie.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression

interface MacroCallInfoFactory {
    fun create(file: CfirFile, expression: CfirMacroExpression): MacroCallInfo
}

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
