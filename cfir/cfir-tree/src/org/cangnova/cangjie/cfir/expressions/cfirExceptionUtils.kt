package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

fun ExceptionAttachmentBuilder.withCfirSymbolEntry(name: String, symbol: CfirBasedSymbol<*>) {
    withCfirEntry("${name}Fir", symbol.cfir)
}
