package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType
import org.cangnova.cangjie.analysis.api.types.CaErrorType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaPrimitiveType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.type

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

@OptIn(CaImplementationDetail::class)
/**
 * 基于底层 Cone 类型和恢复工厂实现的通用公开类型指针。
 */
private class CaGenericTypePointer<C : ConeCangJieType, T : CaType>(
    /**
     * 需要在目标会话中恢复的底层 Cone 类型。
     */
    private val coneType: C,
    /**
     * 从 Cone 类型和目标 CFIR 会话恢复公开类型的工厂函数。
     */
    private val typeFactory: (C, CaCfirSession) -> T?,
) : CaTypePointer<T> {
    /**
     * 在目标 Analysis API 会话中恢复公开类型。
     */
    override fun restore(session: CaSession): T? {
        val cfirSession = session as? CaCfirSession ?: return null
        return typeFactory(coneType, cfirSession)
    }
}

/**
 * 恢复 usual class type 指针。
 */
internal fun restoreUsualClassType(coneType: ConeCangJieType, session: CaCfirSession): CaUsualClassType? =
    coneType.asCaType(session) as? CaUsualClassType

/**
 * 恢复 primitive type 指针。
 */
internal fun restorePrimitiveType(coneType: ConePrimitiveType, session: CaCfirSession): CaPrimitiveType? =
    coneType.asCaType(session) as? CaPrimitiveType

/**
 * 恢复 function type 指针。
 */
internal fun restoreFunctionType(coneType: ConeFunctionType, session: CaCfirSession): CaFunctionType? =
    coneType.asCaType(session) as? CaFunctionType

/**
 * 恢复 tuple type 指针。
 */
internal fun restoreTupleType(coneType: ConeTupleType, session: CaCfirSession): CaTupleType? =
    coneType.asCaType(session) as? CaTupleType

/**
 * 恢复 intersection type 指针。
 */
internal fun restoreIntersectionType(coneType: ConeIntersectionType, session: CaCfirSession): CaIntersectionType? =
    coneType.asCaType(session) as? CaIntersectionType

/**
 * 恢复 union type 指针。
 */
internal fun restoreUnionType(coneType: ConeUnionType, session: CaCfirSession): CaUnionType? =
    coneType.asCaType(session) as? CaUnionType

/**
 * 恢复 type parameter type 指针。
 */
internal fun restoreTypeParameterType(
    coneType: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType,
    session: CaCfirSession,
): CaTypeParameterType? = coneType.asCaType(session) as? CaTypeParameterType

/**
 * 恢复 class-like error type 指针。
 */
internal fun restoreClassErrorType(coneType: ConeErrorType, session: CaCfirSession): CaClassErrorType? =
    coneType.asCaType(session) as? CaClassErrorType

/**
 * 恢复普通 error type 指针。
 */
internal fun restoreErrorType(coneType: ConeCangJieType, session: CaCfirSession): CaErrorType? =
    coneType.asCaType(session) as? CaErrorType

/**
 * 将 Cone 类型转换为公开 Analysis API 类型。
 */
internal fun ConeCangJieType.asCaType(analysisSession: CaCfirSession): CaType =
    analysisSession.cfirSymbolBuilder.typeBuilder.buildType(this)

/**
 * 将 Cone 类型实参转换为公开类型投影列表。
 */
internal fun ConeCangJieType.asPublicTypeProjections(analysisSession: CaCfirSession): List<CaTypeProjection> =
    analysisSession.cfirSymbolBuilder.typeBuilder.buildTypeProjections(this)

/**
 * 将单个 Cone 类型投影转换为公开类型投影。
 */
internal fun ConeTypeProjection.asPublicTypeProjection(analysisSession: CaCfirSession): CaTypeProjection =
    CaTypeProjection(
        type = type.asCaType(analysisSession),
        token = analysisSession.token,
    )

/**
 * 对齐 Kotlin `KaSymbolByFirBuilder.buildAbbreviatedType`：
 * 公开类型统一从 cone attribute 读取 typealias 视图，而不是把裸 `ConeTypeAliasType`
 * 当成 abbreviation 本体。
 */
internal fun org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder.buildAbbreviatedType(
    coneType: ConeCangJieType,
): CaUsualClassType? {
    return coneType.abbreviatedType?.let { abbreviatedConeType ->
        typeBuilder.buildType(abbreviatedConeType) as? CaUsualClassType
    }
}
