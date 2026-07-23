package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.scopes.impl.unwrapOriginalForSubstitutionOverride
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * substitution override 只是 use-site 上为了替换类型实参而生成的声明外壳，
 * 它不应该改变 owner/file/visibility 等声明元数据的来源。
 *
 * provider / symbolProvider 在做归属查询前，必须统一回到原始 callable 符号，
 * 否则同一个成员会因为是否经过 substitution scope 而得到不同结果。
 */
internal fun CfirBasedSymbol<*>.unwrapForDeclarationMetadataLookup(): CfirBasedSymbol<*> {
    return (this as? CfirCallableSymbol<*>)?.unwrapOriginalForSubstitutionOverride() ?: this
}

/**
 * 将 callable symbol 归一化为声明元数据查询使用的原始 callable symbol。
 */
internal fun CfirCallableSymbol<*>.unwrapCallableForDeclarationMetadataLookup(): CfirCallableSymbol<*> {
    return unwrapOriginalForSubstitutionOverride()
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
