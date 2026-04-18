package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.createClassLikeSymbol
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseEmptyAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * CFIR public type 叶子的最小共享协议。
 *
 * 这里对齐 Kotlin `KaFirType`：只暴露底层 `ConeCangJieType`
 * 与当前 `CaCfirSession`，不再引入额外的“通用实现基类”。
 */
internal interface CaCfirType : CaLifetimeOwner {
    val analysisSession: CaCfirSession

    val coneType: ConeCangJieType

    override val token: CaLifetimeToken
        get() = analysisSession.token
}

internal fun CaCfirType.typeEquals(other: Any?): Boolean {
    if (other !is CaCfirType) return false
    return coneType == other.coneType
}

internal fun CaCfirType.typeHashcode(): Int = coneType.hashCode()

/**
 * 当前仓颉 `ClassId` 只表达单段 class-like 声明。
 * 因此 public qualifier 列表目前固定为单段结构。
 */
internal class CaCfirResolvedClassTypeQualifierImpl(
    override val name: org.cangnova.cangjie.name.Name,
    override val typeArguments: List<CaTypeProjection>,
    override val symbol: org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol,
    override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.types.CaResolvedClassTypeQualifier

internal fun CaCfirSession.requireClassLikePublicSymbol(type: ConeCangJieType): CaClassLikeSymbol {
    return typeQueries.queryTypeClassLikeSymbol(type)
        ?.let(::createClassLikeSymbol)
        ?: error("Cannot resolve public class-like symbol for `${type.renderForDebugging()}`")
}

internal fun emptyTypeAnnotations(token: CaLifetimeToken): CaAnnotationList = CaBaseEmptyAnnotationList(token)
