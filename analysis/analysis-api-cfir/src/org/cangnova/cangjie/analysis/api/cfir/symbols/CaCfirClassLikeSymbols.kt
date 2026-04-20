package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolCacheKey
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirClassLikeSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * class / typealias 叶子实现。
 *
 * 对齐 Kotlin 的 `KaFirNamedClassSymbol`、`KaFirTypeAliasSymbol` 落位，
 * 将 class-like 叶子从巨型模型文件中拆出。
 */
internal class CaCfirClassSymbolImpl(
    backingSymbol: CfirClassLikeSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassLikeSymbolBase<CfirClassLikeSymbol<*>>(backingSymbol, analysisSession, containingModule, token), CaClassSymbol {
    override val classKind: CaClassKind
        get() = when (backingSymbol) {
            is CfirClassSymbol -> CaClassKind.CLASS
            is CfirInterfaceSymbol -> CaClassKind.INTERFACE
            is org.cangnova.cangjie.cfir.symbols.CfirStructSymbol -> CaClassKind.STRUCT
            is org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol -> CaClassKind.ENUM
            else -> error("Unsupported class-like symbol `${backingSymbol::class.simpleName}`")
        }

    override val superTypes: List<CaType>
        get() = analysisSession.typeQueries.queryClassLikeSuperTypes(backingSymbol).map { superType -> superType.asCaType(analysisSession) }

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        val stableClassId = classId ?: error("Class symbol `${name}` is missing ClassId")
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(stableClassId), CaAnnotatedSymbol::class.java)
    }
}

internal class CaCfirTypeAliasSymbolImpl(
    backingSymbol: CfirTypeAliasSymbol,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirClassLikeSymbolBase<CfirTypeAliasSymbol>(backingSymbol, analysisSession, containingModule, token), CaTypeAliasSymbol {
    override val expandedType: CaType
        get() = ((backingSymbol.cfir as CfirTypeAlias).expandedTypeRef.coneTypeOrNull?.asCaType(analysisSession))
            ?: error("Cannot build expanded type for `${backingSymbol.classId.asString()}`")

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirClassLikeSymbolPointer(CaCfirClassLikeSymbolCacheKey(backingSymbol.classId), CaAnnotatedSymbol::class.java)
    }
}
