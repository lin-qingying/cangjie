package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder

fun ExceptionAttachmentBuilder.withCfirSymbolIdEntry(name: String, symbol: CfirBasedSymbol<*>?) {
    when (symbol) {
        is CfirClassifierSymbol -> withCfirLookupTagEntry(name, symbol.toLookupTag())
        is CfirCallableSymbol -> withEntry(name, symbol.callableId.toString())
        else -> withEntry(name, symbol.toString())
    }
}

fun ExceptionAttachmentBuilder.withCfirLookupTagEntry(name: String, lookupTag: ConeClassifierLookupTag?) {
    withEntry(name, lookupTag) { tag ->
        when (tag) {
            is ConeClassLikeLookupTag -> tag.classId.asString()
            else -> tag.name.asString()
        }
    }
}

fun ExceptionAttachmentBuilder.withConeTypeEntry(name: String, coneType: ConeCangJieType?) {
    withEntry(name, coneType) {
        buildString { ConeTypeRendererForDebugging(this).render(it) }
    }
}