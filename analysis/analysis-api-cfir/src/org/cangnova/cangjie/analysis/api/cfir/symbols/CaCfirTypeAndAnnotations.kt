package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef


/**
 * 将 CFIR class-like 符号的 resolved super type refs 转换为公开类型列表。
 */
internal fun CfirClassLikeSymbol<*>.superTypesList(builder: CaSymbolByCfirBuilder): List<CaType> = resolvedSuperTypeRefs.mapToCjType(builder)

/**
 * 将 CFIR typeRef 列表批量转换为公开类型列表。
 */
private fun List<CfirTypeRef>.mapToCjType(
    builder: CaSymbolByCfirBuilder,
): List<CaType> = map { typeRef ->
    builder.typeBuilder.buildType(typeRef)
}

/**
 * 将 CFIR callable 的 resolved return type 转换为公开返回类型。
 */
internal fun CfirCallableSymbol<*>.returnType(builder: CaSymbolByCfirBuilder): CaType =
    builder.typeBuilder.buildType(resolvedReturnType)
