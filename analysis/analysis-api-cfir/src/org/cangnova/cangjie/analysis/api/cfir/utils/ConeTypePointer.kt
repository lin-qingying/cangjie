package org.cangnova.cangjie.analysis.api.cfir.utils

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.type

/**
 * 对齐 Kotlin `ConeTypePointer` 的职责边界，但只覆盖仓颉真实存在的 Cone 类型。
 *
 * 不引入任何 Kotlin 专属的 dynamic/flexible/raw/captured 等分支。
 */
internal interface ConeTypePointer<out T : ConeCangJieType> {
    /**
     * 在指定 CFIR Analysis API 会话中恢复 Cone 类型。
     */
    fun restore(session: CaCfirSession): T?
}

/**
 * 为 Cone 类型创建可跨会话恢复的指针。
 */
internal fun <T : ConeCangJieType> T.createPointer(
    builder: CaSymbolByCfirBuilder,
): ConeTypePointer<T> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is ConeClassLikeType -> ConeSimpleTypePointer { session ->
            ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments.map { projection -> projection.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
                isInterface = isInterface,
                isThisType = isThisType,
            )
        }

        is ConeStructType -> ConeSimpleTypePointer { session ->
            ConeStructType(
                lookupTag = lookupTag,
                typeArguments = typeArguments.map { projection -> projection.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
            )
        }

        is ConeEnumType -> ConeSimpleTypePointer { session ->
            ConeEnumType(
                lookupTag = lookupTag,
                typeArguments = typeArguments.map { projection -> projection.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
                isRefEnum = isRefEnum,
            )
        }

        is ConeTypeAliasType -> ConeSimpleTypePointer { session ->
            ConeTypeAliasType(
                classId = classId,
                expandedType = expandedType?.createPointer(builder)?.restore(session),
                typeArguments = typeArguments.map { projection -> projection.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
            )
        }

        is ConePrimitiveType -> ConeSimpleTypePointer { ConePrimitiveType(kind, attributes) }
        is ConeFunctionType -> ConeSimpleTypePointer { session ->
            ConeFunctionType(
                parameterTypes = parameterTypes.map { parameterType -> parameterType.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                returnType = returnType.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null,
                isCFunc = isCFunc,
                isClosureType = isClosureType,
                hasVariableLenArg = hasVariableLenArg,
                attributes = attributes,
            )
        }

        is ConeTupleType -> ConeSimpleTypePointer { session ->
            ConeTupleType(
                elementTypes = elementTypes.map { elementType -> elementType.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
            )
        }

        is ConeIntersectionType -> ConeSimpleTypePointer { session ->
            val upperBoundPointer = upperBoundForApproximation?.createPointer(builder)
            ConeIntersectionType(
                intersectedTypes = intersectedTypes.map { intersectedType -> intersectedType.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                upperBoundForApproximation = upperBoundPointer?.restore(session),
                attributes = attributes,
            )
        }

        is ConeUnionType -> ConeSimpleTypePointer { session ->
            ConeUnionType(
                unionTypes = unionTypes.mapTo(linkedSetOf()) { alternative -> alternative.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
            )
        }

        is ConeQuestType -> ConeSimpleTypePointer { ConeQuestType(attributes) }
        is ConeErrorType -> ConeSimpleTypePointer { session ->
            ConeErrorType(
                diagnostic = diagnostic,
                isUninferredParameter = isUninferredParameter,
                delegatedType = delegatedType?.createPointer(builder)?.restore(session),
                typeArguments = typeArguments.map { projection -> projection.createPointer(builder).restore(session) ?: return@ConeSimpleTypePointer null },
                attributes = attributes,
            )
        }

        is ConeTypeParameterType -> {
            val symbol = lookupTag.typeParameterSymbol
            ConeTypeParameterTypePointer(symbol, attributes)
        }

        else -> error("Unsupported Cone type pointer for `${this::class.qualifiedName}`")
    } as ConeTypePointer<T>
}

/**
 * 基于恢复函数实现的简单 Cone 类型指针。
 */
private class ConeSimpleTypePointer<T : ConeCangJieType>(
    /**
     * 在目标会话中恢复 Cone 类型的函数。
     */
    private val restoreType: (CaCfirSession) -> T?,
) : ConeTypePointer<T> {
    /**
     * 调用恢复函数还原 Cone 类型。
     */
    override fun restore(session: CaCfirSession): T? = restoreType(session)
}

/**
 * 类型参数 Cone 类型的指针。
 */
private class ConeTypeParameterTypePointer(
    /**
     * 原始类型参数符号。
     */
    private val symbol: CfirTypeParameterSymbol,
    /**
     * 类型参数类型携带的 Cone attributes。
     */
    private val attributes: org.cangnova.cangjie.cfir.types.ConeAttributes,
) : ConeTypePointer<ConeTypeParameterType> {
    /**
     * 在目标会话中恢复类型参数符号并重建类型参数 Cone 类型。
     */
    override fun restore(session: CaCfirSession): ConeTypeParameterType? {
        val restoredSymbol = symbol.toLookupTag().toSymbol(session.cfirSession) as? CfirTypeParameterSymbol ?: symbol
        return ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(restoredSymbol), attributes)
    }
}

/**
 * Cone 类型投影的指针。
 */
private class ConeTypeProjectionPointer(
    /**
     * 投影中实际类型的指针。
     */
    private val typePointer: ConeTypePointer< ConeCangJieType>,
) {
    /**
     * 在目标会话中恢复类型投影。
     */
    fun restore(session: CaCfirSession): ConeTypeProjection? = typePointer.restore(session)
}

/**
 * 为 Cone 类型投影创建可恢复指针。
 */
private fun ConeTypeProjection.createPointer(builder: CaSymbolByCfirBuilder): ConeTypeProjectionPointer {
    return ConeTypeProjectionPointer(type.createPointer(builder))
}
