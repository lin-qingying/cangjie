package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag

/**
 * 将 class-like 声明还原为“声明自身类型”。
 *
 * 类型系统在按具体 use-site 类型计算父类型时，需要先得到一份带声明型类型参数的 self type，
 * 再交给类型层 supertype provider 做实例化与 extend 传播。
 */
internal fun declarationSelfType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType? {
    if (!symbol.isBound) return null

    val declarationTypeArguments = symbol.cfir.typeParameters.map { typeParameter ->
        ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
    }

    return when (symbol) {
        is CfirInterfaceSymbol -> ConeClassLikeType(
            lookupTag = symbol.toLookupTag(),
            typeArguments = declarationTypeArguments,
            isInterface = true,
        )
        is CfirStructSymbol -> ConeStructType(
            lookupTag = symbol.toLookupTag(),
            typeArguments = declarationTypeArguments,
        )
        is CfirEnumSymbol -> ConeEnumType(
            lookupTag = symbol.toLookupTag(),
            typeArguments = declarationTypeArguments,
            isRefEnum = symbol.isRefEnum,
        )
        else -> ConeClassLikeType(
            lookupTag = symbol.toLookupTag(),
            typeArguments = declarationTypeArguments,
        )
    }
}
