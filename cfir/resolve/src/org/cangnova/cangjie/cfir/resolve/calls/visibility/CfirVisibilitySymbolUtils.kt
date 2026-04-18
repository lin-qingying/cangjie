package org.cangnova.cangjie.cfir.resolve.calls.visibility

import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.ClassId

fun CfirBasedSymbol<*>.getOwnerClassId(provider: CfirProvider): ClassId? = when (this) {
    is CfirClassLikeSymbol<*> -> null
    is CfirCallableSymbol<*> -> provider.getContainingClassId(this) ?: callableId.classId
    else -> null
}

fun CfirBasedSymbol<*>.isVariableOrNamedFunction(): Boolean {
    return this is CfirVariableSymbol<*> || this is CfirNamedFunctionSymbol
}
