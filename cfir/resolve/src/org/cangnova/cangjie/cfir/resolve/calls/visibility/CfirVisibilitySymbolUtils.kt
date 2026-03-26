package org.cangnova.cangjie.cfir.resolve.calls.visibility

import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClassId
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.ClassId

fun CfirSymbol<*>.getOwnerClassId(provider: CfirProvider): ClassId? = when (this) {
    is CfirClassLikeSymbol<*> -> classId.outerClassId
    is CfirCallableSymbol<*> -> provider.getContainingClassId(this)
    else -> null
}

fun CfirSymbol<*>.isVariableOrNamedFunction(): Boolean {
    return this is CfirVariableSymbol<*> || this is CfirNamedFunctionSymbol
}
