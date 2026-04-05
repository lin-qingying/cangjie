package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.analysis.api.types.pointers.CaTypePointer
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.name.ClassId

/**
 * 所有公开 CFIR 类型共享的最小包装。
 */
internal open class CaCfirTypeImpl(
    override val token: CaLifetimeToken,
    val coneType: ConeCangJieType,
) : CaType {
    override val presentation: String
        get() = coneType.renderForDebugging()
}

internal class CaCfirClassLikeTypeImpl(
    token: CaLifetimeToken,
    coneType: ConeCangJieType,
) : CaCfirTypeImpl(token, coneType), CaClassLikeType {
    override val classId: ClassId
        get() = when (val currentType = coneType) {
            is ConeClassLikeType -> currentType.classId
            is ConeStructType -> currentType.classId
            is ConeEnumType -> currentType.classId
            is ConeTypeAliasType -> currentType.classId
            is ConePrimitiveType -> currentType.kind.classId
            else -> error("仅 class-like 类型支持 classId：${currentType::class.simpleName}")
        }

    override val typeArguments: List<CaType>
        get() = when (val currentType = coneType) {
            is ConeClassLikeType -> currentType.typeArguments
            is ConeStructType -> currentType.typeArguments
            is ConeEnumType -> currentType.typeArguments
            is ConeTypeAliasType -> currentType.typeArguments
            is ConePrimitiveType -> emptyList()
            else -> error("仅 class-like 类型支持类型实参：${currentType::class.simpleName}")
        }.mapNotNull { projection -> projection.type?.asCaType(token) }

    /**
     * 公开类型包装本身不持有 session。
     * class-like 声明恢复统一交给类型信息组件处理。
     */
    override val symbol: CaClassLikeSymbol?
        get() = null
}

internal class CaCfirFunctionTypeImpl(
    token: CaLifetimeToken,
    coneType: ConeFuncType,
) : CaCfirTypeImpl(token, coneType), CaFunctionType {
    private val functionConeType: ConeFuncType
        get() = coneType as ConeFuncType

    override val parameterTypes: List<CaType>
        get() = functionConeType.parameterTypes.map { type -> type.asCaType(token) }

    override val returnType: CaType
        get() = functionConeType.returnType.asCaType(token)

    override val isCFunction: Boolean
        get() = functionConeType.isCFunc

    override val isClosureType: Boolean
        get() = functionConeType.isClosureType

    override val hasVariableLengthArgument: Boolean
        get() = functionConeType.hasVariableLenArg
}

internal class CaCfirTupleTypeImpl(
    token: CaLifetimeToken,
    coneType: ConeTupleType,
) : CaCfirTypeImpl(token, coneType), CaTupleType {
    private val tupleConeType: ConeTupleType
        get() = coneType as ConeTupleType

    override val elementTypes: List<CaType>
        get() = tupleConeType.elementTypes.map { type -> type.asCaType(token) }
}

internal class CaCfirIntersectionTypeImpl(
    token: CaLifetimeToken,
    coneType: ConeIntersectionType,
) : CaCfirTypeImpl(token, coneType), CaIntersectionType {
    private val intersectionConeType: ConeIntersectionType
        get() = coneType as ConeIntersectionType

    override val conjuncts: List<CaType>
        get() = intersectionConeType.intersectedTypes.map { type -> type.asCaType(token) }
}

internal class CaCfirUnionTypeImpl(
    token: CaLifetimeToken,
    coneType: ConeUnionType,
) : CaCfirTypeImpl(token, coneType), CaUnionType {
    private val unionConeType: ConeUnionType
        get() = coneType as ConeUnionType

    override val alternatives: List<CaType>
        get() = unionConeType.unionTypes.map { type -> type.asCaType(token) }
}

/**
 * 当前 CFIR 类型指针直接保存语义类型对象本身，恢复时重新绑定新的 lifetime。
 */
internal class CaCfirTypePointer(
    private val coneType: ConeCangJieType,
) : CaTypePointer<CaType> {
    override fun restoreType(session: org.cangnova.cangjie.analysis.api.CaSession): CaType? {
        val cfirSession = session as? CaCfirSession ?: return null
        return coneType.asCaType(cfirSession.token)
    }
}

internal fun ConeCangJieType.asCaType(token: CaLifetimeToken): CaType = when (this) {
    is ConeClassLikeType,
    is ConeStructType,
    is ConeEnumType,
    is ConeTypeAliasType,
    is ConePrimitiveType,
    -> CaCfirClassLikeTypeImpl(token, this)

    is ConeFuncType -> CaCfirFunctionTypeImpl(token, this)
    is ConeTupleType -> CaCfirTupleTypeImpl(token, this)
    is ConeIntersectionType -> CaCfirIntersectionTypeImpl(token, this)
    is ConeUnionType -> CaCfirUnionTypeImpl(token, this)
    else -> CaCfirTypeImpl(token, this)
}
