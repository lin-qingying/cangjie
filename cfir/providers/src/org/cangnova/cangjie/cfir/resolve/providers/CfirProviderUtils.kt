package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.scopes.impl.unwrapOriginalForSubstitutionOverride
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

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

internal fun CfirCallableSymbol<*>.unwrapCallableForDeclarationMetadataLookup(): CfirCallableSymbol<*> {
    return unwrapOriginalForSubstitutionOverride()
}
