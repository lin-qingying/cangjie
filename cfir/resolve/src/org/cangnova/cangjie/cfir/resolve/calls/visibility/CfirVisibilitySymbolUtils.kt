package org.cangnova.cangjie.cfir.resolve.calls.visibility

import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.ClassId

/** 获取符号所属 owner classId；顶层或 class-like 自身没有 owner 时返回 null。 */
fun CfirBasedSymbol<*>.getOwnerClassId(provider: CfirProvider): ClassId? = when (this) {
    is CfirClassLikeSymbol<*> -> null
    is CfirCallableSymbol<*> -> provider.getContainingClass(this)?.classId ?: callableId.classId
    else -> null
}

/** 判断符号是否为变量或命名函数，用于可见性分支过滤 callable 类别。 */
fun CfirBasedSymbol<*>.isVariableOrNamedFunction(): Boolean {
    return this is CfirVariableSymbol<*> || this is CfirNamedFunctionSymbol
}
