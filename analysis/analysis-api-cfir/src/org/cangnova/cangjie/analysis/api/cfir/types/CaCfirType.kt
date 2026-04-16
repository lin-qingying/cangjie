package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.createClassLikeSymbol
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseEmptyAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
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
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
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

/**
 * Kotlin FIR 侧有 `createTypePointer(...)` helper。
 * 仓颉侧保持同样分层：pointer 只是内部恢复工具，不再作为 public type 通用基类存在。
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

internal fun restoreTypeParameterType(coneType: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType, session: CaCfirSession): CaTypeParameterType? =
    coneType.asCaType(session) as? CaTypeParameterType

internal fun restoreClassErrorType(coneType: ConeErrorType, session: CaCfirSession): CaClassErrorType? =
    coneType.asCaType(session) as? CaClassErrorType

internal fun restoreErrorType(coneType: ConeCangJieType, session: CaCfirSession): CaErrorType? =
    coneType.asCaType(session) as? CaErrorType

internal fun ConeCangJieType.asCaType(analysisSession: CaCfirSession): CaType = when (this) {
    is ConeClassLikeType,
    is ConeStructType,
    is ConeEnumType,
    is ConeTypeAliasType,
    is ConePrimitiveType,
    -> CaCfirUsualClassType(this, analysisSession)

    is ConeFuncType -> CaCfirFunctionType(this, analysisSession)
    is ConeTupleType -> CaCfirTupleType(this, analysisSession)
    is ConeIntersectionType -> CaCfirIntersectionType(this, analysisSession)
    is ConeUnionType -> CaCfirUnionType(this, analysisSession)
    is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType -> CaCfirTypeParameterType(this, analysisSession)
    is ConeErrorType -> CaCfirClassErrorType(this, analysisSession)
    is ConeQuestType -> CaCfirNonClassErrorType(
        coneType = this,
        analysisSession = analysisSession,
        errorMessageImpl = "Quest type cannot be exposed as a stable public type",
        presentableTextImpl = renderForDebugging(),
    )

    else -> error("Unsupported CFIR public type projection: ${this::class.qualifiedName}")
}

internal fun ConeCangJieType.asPublicTypeProjections(analysisSession: CaCfirSession): List<CaTypeProjection> {
    val coneArguments: List<ConeTypeProjection> = when (this) {
        is ConeClassLikeType -> typeArguments
        is ConeStructType -> typeArguments
        is ConeEnumType -> typeArguments
        is ConeTypeAliasType -> typeArguments
        is ConePrimitiveType -> emptyList()
        else -> error("Only class-like CFIR types can expose type arguments: ${this::class.simpleName}")
    }
    return coneArguments.map { projection ->
        CaTypeProjection(
            type = projection.type?.asCaType(analysisSession),
            token = analysisSession.token,
        )
    }
}

internal fun CaCfirSession.requireClassLikePublicSymbol(type: ConeCangJieType): CaClassLikeSymbol {
    return queryTypeClassLikeSymbol(type)
        ?.let(::createClassLikeSymbol)
        ?: error("Cannot resolve public class-like symbol for `${type.renderForDebugging()}`")
}

internal fun emptyTypeAnnotations(token: CaLifetimeToken): CaAnnotationList = CaBaseEmptyAnnotationList(token)
