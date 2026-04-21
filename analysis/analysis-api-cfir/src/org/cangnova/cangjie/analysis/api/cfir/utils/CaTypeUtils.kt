package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType

/**
 * 对齐 Kotlin `analysis-api-fir/utils/typeUtils.kt` 的 type helper 集合。
 *
 * 这里统一承载：
 * 1. `ConeCangJieType -> CaType` 的公开转换入口；
 * 2. 公开 `CaTypePointer` 的通用恢复 helper；
 * 3. 类型投影从 CFIR 到 public model 的统一转换。
 */
internal fun <C : ConeCangJieType, T : CaType> createTypePointer(
    coneType: C,
    typeFactory: (C, CaCfirSession) -> T?,
): CaTypePointer<T> = CaGenericTypePointer(coneType, typeFactory)

private class CaGenericTypePointer<C : ConeCangJieType, T : CaType>(
    private val coneType: C,
    private val typeFactory: (C, CaCfirSession) -> T?,
) : CaTypePointer<T> {
    override fun restoreType(session: CaSession): T? {
        val cfirSession = session as? CaCfirSession ?: return null
        return typeFactory(coneType, cfirSession)
    }
}

internal fun restoreUsualClassType(coneType: ConeCangJieType, session: CaCfirSession): CaUsualClassType? =
    coneType.asCaType(session) as? CaUsualClassType

internal fun restoreFunctionType(coneType: ConeFuncType, session: CaCfirSession): CaFunctionType? =
    coneType.asCaType(session) as? CaFunctionType

internal fun restoreTupleType(coneType: ConeTupleType, session: CaCfirSession): CaTupleType? =
    coneType.asCaType(session) as? CaTupleType

internal fun restoreIntersectionType(coneType: ConeIntersectionType, session: CaCfirSession): CaIntersectionType? =
    coneType.asCaType(session) as? CaIntersectionType

internal fun restoreUnionType(coneType: ConeUnionType, session: CaCfirSession): CaUnionType? =
    coneType.asCaType(session) as? CaUnionType

internal fun restoreTypeParameterType(
    coneType: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType,
    session: CaCfirSession,
): CaTypeParameterType? = coneType.asCaType(session) as? CaTypeParameterType

internal fun restoreClassErrorType(coneType: ConeErrorType, session: CaCfirSession): CaClassErrorType? =
    coneType.asCaType(session) as? CaClassErrorType

internal fun restoreErrorType(coneType: ConeCangJieType, session: CaCfirSession): CaErrorType? =
    coneType.asCaType(session) as? CaErrorType

internal fun ConeCangJieType.asCaType(analysisSession: CaCfirSession): CaType =
    analysisSession.cfirSymbolBuilder.typeBuilder.buildType(this)

internal fun ConeCangJieType.asPublicTypeProjections(analysisSession: CaCfirSession): List<CaTypeProjection> =
    analysisSession.cfirSymbolBuilder.typeBuilder.buildTypeProjections(this)

internal fun ConeTypeProjection.asPublicTypeProjection(analysisSession: CaCfirSession): CaTypeProjection =
    CaTypeProjection(
        type = type?.asCaType(analysisSession),
        token = analysisSession.token,
    )
