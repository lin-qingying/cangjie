package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.psi

/**
 * CFIR public symbol 创建入口。
 *
 * 这里负责：
 * 1. 为 public symbol 选择稳定 cache key；
 * 2. 把真正的构造委托给 `CaSymbolByCfirBuilder`；
 * 3. 保证 session 内相同语义声明始终复用同一 public symbol。
 *
 * 这样创建策略与 restore/query 分层清晰，不再把这些职责堆进一个 `Factory` 文件。
 */
internal fun CaCfirSession.createPackageSymbol(fqName: FqName): CaPackageSymbol =
    getOrCreatePublicSymbol(CaCfirPackageSymbolCacheKey(fqName)) {
        constructPackageSymbol(fqName)
    }

internal fun CaCfirSession.createFileSymbol(file: CjFile): CaFileSymbol =
    getOrCreatePublicSymbol(CaCfirFileSymbolCacheKey(file)) {
        constructFileSymbol(file)
    }

internal fun CaCfirSession.createClassLikeSymbol(symbol: CfirClassLikeSymbol<*>): CaClassLikeSymbol =
    getOrCreatePublicSymbol(CaCfirClassLikeSymbolCacheKey(symbol.classId)) {
        cfirSymbolBuilder.classifierBuilder.buildClassLikeSymbol(symbol)
    }

internal fun CaCfirSession.createExtendSymbol(symbol: CfirExtendSymbol): CaExtendSymbol {
    val identity = resolveExtendIdentity(symbol)
    return getOrCreatePublicSymbol(CaCfirExtendSymbolCacheKey(identity.stableIdentity)) {
        constructExtendSymbol(symbol)
    }
}

internal fun CaCfirSession.createCallableSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol {
    val cacheKey = symbol.publicSymbolCacheKeyOrNull(this)
    return if (cacheKey != null) {
        getOrCreatePublicSymbol(cacheKey) {
            cfirSymbolBuilder.callableBuilder.buildCallableSymbol(symbol)
        }
    } else {
        cfirSymbolBuilder.callableBuilder.buildCallableSymbol(symbol)
    }
}

internal fun CaCfirSession.createTypeParameterSymbol(symbol: CfirTypeParameterSymbol): CaTypeParameterSymbol {
    val ownerKey = symbol.publicTypeParameterOwnerKey(this)
    if (ownerKey == null) {
        val psi = symbol.cfir.source?.psi ?: error("Type parameter `${symbol.name}` requires source PSI")
        return getOrCreatePublicSymbol(CaCfirPsiSymbolCacheKey(psi, CaCfirPsiSymbolKind.TYPE_PARAMETER)) {
            cfirSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(symbol)
        }
    }
    val parameterIndex = symbol.stableTypeParameterIndex()
        ?: error("Type parameter `${symbol.name}` is missing a stable owner index")
    return getOrCreatePublicSymbol(CaCfirTypeParameterSymbolCacheKey(ownerKey, symbol.name, parameterIndex)) {
        cfirSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(symbol)
    }
}

internal fun CaCfirSession.createValueParameterSymbol(symbol: CfirValueParameter): CaValueParameterSymbol {
    return createValueParameterSymbol(symbol.symbol)
}

internal fun CaCfirSession.createValueParameterSymbol(
    ownerSymbol: CaValueParameterOwnerSymbol,
    parameter: CfirValueParameter,
    parameterIndex: Int,
): CaValueParameterSymbol {
    val ownerKey = ownerSymbol.publicSymbolCacheKeyOrNull()
    return if (ownerKey != null) {
        getOrCreatePublicSymbol(
            CaCfirValueParameterSymbolCacheKey(
                ownerKey = ownerKey,
                parameterIndex = parameterIndex,
                parameterName = parameter.name,
            ),
        ) {
            cfirSymbolBuilder.variableBuilder.buildOwnedValueParameterSymbol(ownerSymbol, parameter, parameterIndex)
        }
    } else {
        createValueParameterSymbol(parameter.symbol)
    }
}

internal fun CaCfirSession.createValueParameterSymbol(symbol: CfirValueParameterSymbol): CaValueParameterSymbol {
    val psi = symbolQueries.lookupSourcePsi(symbol)
    val cacheKey = psi?.let { CaCfirPsiSymbolCacheKey(it, CaCfirPsiSymbolKind.LOCAL_VARIABLE) }
    return if (cacheKey != null) {
        getOrCreatePublicSymbol(cacheKey) {
            cfirSymbolBuilder.variableBuilder.buildValueParameterSymbol(symbol)
        }
    } else {
        cfirSymbolBuilder.variableBuilder.buildValueParameterSymbol(symbol)
    }
}

internal fun CaCfirSession.createPropertyAccessorSymbol(
    backingSymbol: CfirCallableSymbol<*>,
    ownerSymbol: CaPropertySymbol,
    kind: CaCfirPropertyAccessorKind,
): CaSymbol {
    val ownerKey = ownerSymbol.publicSymbolCacheKeyOrNull()
        ?: error("Property accessor owner must expose a stable public key")
    return getOrCreatePublicSymbol(CaCfirPropertyAccessorSymbolCacheKey(ownerKey, kind)) {
        cfirSymbolBuilder.functionBuilder.buildPropertyAccessorSymbol(backingSymbol, ownerSymbol, kind)
    }
}

/**
 * 这些 `construct*` helper 只负责“按既定语义构造 public symbol 实例”，
 * 不负责 cache key 选择，也不负责 restore/query。
 *
 * 这样 `CaSymbolByCfirBuilder` 可以专注在 classifier/callable/type 的叶子构造，
 * package/file/extend 这类更靠近 session symbol 子系统入口的构造，
 * 就落回 `cfir.symbols`，避免 builder 继续向“中心化工厂”膨胀。
 */
internal fun CaCfirSession.constructPackageSymbol(fqName: FqName): CaPackageSymbol =
    CaCfirPackageSymbol(fqName, useSiteModule, token)

internal fun CaCfirSession.constructFilePublicSymbol(symbol: CfirFileSymbol): CaFileSymbol {
    val file = symbolQueries.lookupContainingFile(symbol)
        ?: error("File symbol `${symbol}` is missing recoverable CjFile source")
    return constructFileSymbol(file)
}

internal fun CaCfirSession.constructFileSymbol(file: CjFile): CaFileSymbol {
    val fileSymbol = symbolQueries.lookupFileSymbol(file)
        ?: error("Cannot build low-level file symbol for `${file.name}`")
    return CaCfirFileSymbol(fileSymbol, file, useSiteModule, token)
}

internal fun CaCfirSession.constructExtendSymbol(symbol: CfirExtendSymbol): CaExtendSymbol {
    val identity = resolveExtendIdentity(symbol)
    return CaCfirExtendSymbol(
        symbol,
        identity.extendPsi,
        identity.stableIdentity,
        identity.extendId,
        identity.packageFqName,
        this,
        useSiteModule,
        token,
    )
}
