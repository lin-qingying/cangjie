package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirTopLevelPublicSymbolQueryValue
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * CFIR public symbol 查询入口。
 *
 * 这里负责把“查询某类 public symbol”表达为稳定 session API：
 * 1. 统一使用 `CaSymbolByCfirBuilder` 构造 public symbol；
 * 2. 统一通过 session cache 复用同一 public 实例；
 * 3. 不再保留一个中心化 `Factory` 文件混合构造与查询职责。
 */
internal fun CaCfirSession.getPublicSymbol(symbol: CfirBasedSymbol<*>): CaSymbol =
    when (symbol) {
        is CfirClassLikeSymbol<*> -> createClassLikeSymbol(symbol)
        is CfirCallableSymbol<*> -> createCallableSymbol(symbol)
        is CfirTypeParameterSymbol -> createTypeParameterSymbol(symbol)
        is CfirFileSymbol -> createFilePublicSymbol(symbol)
        is CfirExtendSymbol -> createExtendSymbol(symbol)
        else -> error("Unsupported public symbol query for `${symbol::class.simpleName}`")
    }

internal fun CaCfirSession.getPackagePublicSymbol(fqName: FqName): CaPackageSymbol? {
    if (!scopeQueries.hasVisiblePackage(fqName)) return null
    return createPackageSymbol(fqName)
}

internal fun CaCfirSession.getClassLikePublicSymbol(classId: ClassId): CaClassLikeSymbol? {
    val symbol = symbolQueries.lookupClassLikeSymbol(classId) ?: return null
    return createClassLikeSymbol(symbol)
}

internal fun CaCfirSession.getClassPublicSymbol(classId: ClassId): CaClassSymbol? =
    getClassLikePublicSymbol(classId) as? CaClassSymbol

internal fun CaCfirSession.getTypeAliasPublicSymbol(classId: ClassId): CaTypeAliasSymbol? =
    getClassLikePublicSymbol(classId) as? CaTypeAliasSymbol

internal fun CaCfirSession.createFilePublicSymbol(symbol: org.cangnova.cangjie.cfir.symbols.CfirFileSymbol): CaFileSymbol {
    return constructFilePublicSymbol(symbol)
}

internal fun CaCfirSession.getOrCreateTopLevelPublicSymbols(
    packageFqName: FqName,
    name: Name,
): CaCfirTopLevelPublicSymbolQueryValue {
    return getOrCreateTopLevelSymbolQuery(packageFqName, name) {
        val queryResult = symbolQueries.queryTopLevelSymbols(packageFqName, name)
        CaCfirTopLevelPublicSymbolQueryValue(
            classLikeSymbols = queryResult.classLikeSymbols.map(::createClassLikeSymbol),
            callableSymbols = queryResult.callableSymbols.map(::createCallableSymbol),
        )
    }
}

internal fun CaCfirSession.getTopLevelExtendPublicSymbols(packageFqName: FqName): List<CaExtendSymbol> =
    cfirSession.extendProviderOrNull
        ?.getExtendsInPackage(packageFqName)
        ?.map { extend -> createExtendSymbol(extend.symbol) }
        .orEmpty()

internal fun CaCfirSession.getExtendPublicSymbols(targetClassId: ClassId): List<CaExtendSymbol> =
    cfirSession.extendProviderOrNull
        ?.getExtendsForClass(targetClassId)
        ?.map { extend -> createExtendSymbol(extend.symbol) }
        .orEmpty()
