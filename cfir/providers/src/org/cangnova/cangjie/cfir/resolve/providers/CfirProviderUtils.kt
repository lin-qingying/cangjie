package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.scopes.impl.unwrapOriginalForSubstitutionOverride
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
