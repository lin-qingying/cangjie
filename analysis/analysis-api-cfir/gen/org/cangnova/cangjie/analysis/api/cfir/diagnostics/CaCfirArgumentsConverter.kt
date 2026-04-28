

@file:Suppress("UNUSED_PARAMETER")

package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

internal fun convertArgument(argument: Any?, analysisSession: CaCfirSession): Any? {
    return convertArgument(argument, analysisSession.cfirSymbolBuilder)
}

private fun convertArgument(argument: Any?, cfirSymbolBuilder: CaSymbolByCfirBuilder): Any? {
    return when (argument) {
        null -> null
        is CfirTypeParameterSymbol -> convertArgument(argument, cfirSymbolBuilder)
        is ConeCangJieType -> convertArgument(argument, cfirSymbolBuilder)
        is Collection<*> -> convertArgument(argument, cfirSymbolBuilder)
        else -> argument
    }
}

private fun convertArgument(argument: CfirTypeParameterSymbol, cfirSymbolBuilder: CaSymbolByCfirBuilder): Any? {
    return cfirSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(argument)
}

private fun convertArgument(argument: ConeCangJieType, cfirSymbolBuilder: CaSymbolByCfirBuilder): Any? {
    return cfirSymbolBuilder.typeBuilder.buildType(argument)
}

private fun convertArgument(argument: Collection<*>, cfirSymbolBuilder: CaSymbolByCfirBuilder): Any? {
    return argument.map { value ->
        convertArgument(value, cfirSymbolBuilder)
    }
}

