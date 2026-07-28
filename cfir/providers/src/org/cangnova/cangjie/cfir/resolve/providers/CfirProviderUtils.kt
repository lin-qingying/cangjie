package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol

/**
 * accessor、fake override、delegated override 与 substitution override 都只是 use-site 外壳，
 * 不应该改变 owner/file/visibility/annotation 等声明元数据的来源。
 *
 * provider / symbolProvider 在做归属查询前，必须先把 accessor 还原为 property，再递归剥离
 * override 外壳，最后再次还原可能由 override 链暴露出的 accessor。这个顺序保证属性访问与
 * 直接属性查询最终落到同一个声明身份。
 */
internal fun CfirBasedSymbol<*>.unwrapForDeclarationMetadataLookup(): CfirBasedSymbol<*> {
    return (this as? CfirCallableSymbol<*>)?.unwrapCallableForDeclarationMetadataLookup() ?: this
}

/**
 * 将 callable symbol 归一化为声明元数据查询使用的原始 callable symbol。
 */
internal fun CfirCallableSymbol<*>.unwrapCallableForDeclarationMetadataLookup(): CfirCallableSymbol<*> {
    val propertyOrCallable = unwrapPropertyAccessor()
    val original = propertyOrCallable.unwrapFakeOverridesOrDelegated()
    val declarationIdentity = original.unwrapPropertyAccessor()
    return declarationIdentity.unwrapNonLocalPatternBinding()
}

/** 将属性访问器归一化为其拥有的属性符号。 */
private fun CfirCallableSymbol<*>.unwrapPropertyAccessor(): CfirCallableSymbol<*> =
    (this as? CfirPropertyAccessorSymbol)?.propertySymbol ?: this

/**
 * 顶层 pattern 声明进入名称作用域的是 binding symbol，但声明注解和 initializer 的 owner
 * 是外层 [org.cangnova.cangjie.cfir.declarations.CfirPatternVariable]。非局部 binding 必须
 * 通过 provider 索引还原到该 owner，才能让所有声明元数据查询看到同一个声明身份。
 */
private fun CfirCallableSymbol<*>.unwrapNonLocalPatternBinding(): CfirCallableSymbol<*> {
    val binding = this as? CfirPatternBindingSymbol ?: return this
    if (binding.cfir.isLocal) return binding
    val owner = checkNotNull(binding.cfir.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding)) {
        "Non-local pattern binding `${binding.callableId}` is missing its declaration owner index"
    }
    return owner.symbol
}

/**
 * 统一 provider 侧 container file 查询入口。
 */
fun CfirProvider.getContainingFile(symbol: CfirBasedSymbol<*>): CfirFile? {
    val normalizedSymbol = symbol.unwrapForDeclarationMetadataLookup()
    return when (normalizedSymbol) {
        is CfirCallableSymbol<*> -> getCfirCallableContainerFile(normalizedSymbol)
        is CfirClassLikeSymbol<*> -> getCfirClassifierContainerFileIfAny(normalizedSymbol)
        else -> null
    }
}

/**
 * 从声明符号自身所属的 session/provider 查询容器文件。
 *
 * 声明元数据的 owner 是声明模块，不是当前使用点模块。调用方不得把 use-site provider
 * 传入归属查询，否则依赖模块声明会因主模块 provider 不持有其文件索引而被误判为无归属。
 */
fun CfirBasedSymbol<*>.getContainingFile(): CfirFile? {
    val normalizedSymbol = unwrapForDeclarationMetadataLookup()
    return normalizedSymbol.cfir.moduleData.session.cfirProvider.getContainingFile(normalizedSymbol)
}

/**
 * 从声明符号自身所属的 session/provider 查询外层 class-like 声明。
 */
fun CfirBasedSymbol<*>.getContainingClass(): CfirClassLikeSymbol<*>? {
    val normalizedSymbol = unwrapForDeclarationMetadataLookup()
    return normalizedSymbol.cfir.moduleData.session.cfirProvider.getContainingClass(normalizedSymbol)
}

/**
 * 从 callable 原始声明所属的 session/provider 查询外层 extend 声明。
 */
fun CfirCallableSymbol<*>.getContainingExtend(): CfirExtend? {
    val normalizedSymbol = unwrapCallableForDeclarationMetadataLookup()
    return normalizedSymbol.cfir.moduleData.session.extendProviderOrNull?.getContainingExtend(normalizedSymbol)
}
