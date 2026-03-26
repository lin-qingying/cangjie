package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.ClassId

fun CfirProvider.getContainingFile(symbol: CfirSymbol<*>): CfirFile? =
    symbolProvider.getContainingFile(symbol)

fun CfirProvider.getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? =
    symbolProvider.getContainingClassId(symbol)
