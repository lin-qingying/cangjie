package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol

/**
 * 将 class-like 声明还原为“声明自身类型”。
 *
 * 类型系统在按具体 use-site 类型计算父类型时，需要先得到一份带声明型类型参数的 self type，
 * 再交给类型层 supertype provider 做实例化与 extend 传播。
 */
fun declarationSelfType(symbol: CfirClassLikeSymbol<*>, constructorTag: ConeClassLikeLookupTag = symbol.toLookupTag()): ConeCangJieType? {
    if (!symbol.isBound) return null

    val declarationTypeArguments = symbol.cfir.typeParameters.map { typeParameter ->
        ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
    }

    // 当前类型 constructor 可携带真实 CJO fixed symbol；self 派生保留该身份及其 own TP。
    return when (symbol) {
        is CfirClassSymbol, is CfirTypeAliasSymbol -> ConeClassLikeType(constructorTag, declarationTypeArguments)
        is CfirInterfaceSymbol -> ConeClassLikeType(constructorTag, declarationTypeArguments, isInterface = true)
        is CfirStructSymbol -> ConeStructType(constructorTag, declarationTypeArguments)
        is CfirEnumSymbol -> ConeEnumType(constructorTag, declarationTypeArguments, isRefEnum = symbol.isRefEnum)
        else -> symbol.constructType(declarationTypeArguments)
    }
}
