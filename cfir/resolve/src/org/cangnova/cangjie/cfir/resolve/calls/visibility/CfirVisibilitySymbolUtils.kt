package org.cangnova.cangjie.cfir.resolve.calls.visibility

import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.ClassId

/**
 * 获取符号所属 owner classId；顶层或 class-like 自身没有 owner 时返回 null。
 *
 * owner 元数据必须通过声明符号自身的 session/provider 查询，不能由使用点传入 provider。
 */
fun CfirBasedSymbol<*>.getOwnerClassId(): ClassId? = when (this) {
    is CfirClassLikeSymbol<*> -> null
    is CfirCallableSymbol<*> -> getContainingClass()?.classId ?: callableId.classId
    else -> null
}

/** 判断符号是否为变量或命名函数，用于可见性分支过滤 callable 类别。 */
fun CfirBasedSymbol<*>.isVariableOrNamedFunction(): Boolean {
    return this is CfirVariableSymbol<*> || this is CfirNamedFunctionSymbol
}
