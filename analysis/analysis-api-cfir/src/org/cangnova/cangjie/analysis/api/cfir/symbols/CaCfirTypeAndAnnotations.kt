package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef


internal fun CfirClassLikeSymbol<*>.superTypesList(builder: CaSymbolByCfirBuilder): List<CaType> = resolvedSuperTypeRefs.mapToCjType(builder)

private fun List<CfirTypeRef>.mapToCjType(
    builder: CaSymbolByCfirBuilder,
): List<CaType> = map { typeRef ->
    builder.typeBuilder.buildType(typeRef)
}

internal fun CfirCallableSymbol<*>.returnType(builder: CaSymbolByCfirBuilder): CaType =
    builder.typeBuilder.buildType(resolvedReturnType)

