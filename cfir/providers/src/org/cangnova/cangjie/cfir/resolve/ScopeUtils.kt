package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType

fun CfirClassLikeSymbol<*>.defaultType(): ConeClassLikeType = cfir.defaultType()
fun CfirClassLikeDeclaration.defaultType(): ConeClassLikeType =
    ConeClassLikeType (
        symbol.toLookupTag(),
        typeParameters.map {
            ConeTypeParameterTypeImpl(
                it.symbol.toLookupTag(),

                )
        }

    )
