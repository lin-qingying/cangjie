package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType

/**
 * 基于 class-like symbol 的当前 CFIR 声明构造默认类型。
 */
fun CfirClassLikeSymbol<*>.defaultType(): ConeClassLikeType = cfir.defaultType()

/**
 * 为 class-like 声明构造以自身类型参数为实参的默认 class-like 类型。
 */
fun CfirClassLikeDeclaration.defaultType(): ConeClassLikeType =
    ConeClassLikeType (
        symbol.toLookupTag(),
        typeParameters.map {
            ConeTypeParameterTypeImpl(
                it.symbol.toLookupTag(),

                )
        }

    )
