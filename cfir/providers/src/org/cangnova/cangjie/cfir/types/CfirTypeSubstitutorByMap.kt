package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType

/**
 * 基于声明侧类型参数替换表构造的 ConeSubstitutor。
 *
 * 该能力属于通用类型系统基础设施，应放在 providers 层供 supertype 展开、
 * use-site scope 和调用解析共享，而不是局限在 resolve 某个阶段。
 */
class CfirTypeSubstitutorByMap(
    private val replacements: Map<String, ConeCangJieType>,
) : ConeSubstitutor() {
    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
        return when (type) {
            is ConeTypeParameterType -> replacements[type.lookupTag.name.asString()]
            is ConeClassLikeType -> substituteArguments(type.typeArguments)?.let { arguments ->
                ConeClassLikeType(type.lookupTag, arguments, type.attributes, type.isInterface, type.isThisType)
            }
            is ConeStructType -> substituteArguments(type.typeArguments)?.let { arguments ->
                ConeStructType(type.lookupTag, arguments, type.attributes)
            }
            is ConeEnumType -> substituteArguments(type.typeArguments)?.let { arguments ->
                ConeEnumType(type.lookupTag, arguments, type.attributes, type.isRefEnum)
            }
            is ConeFuncType -> substituteFunction(type)
            is ConeTupleType -> substituteTypes(type.elementTypes)?.let { elements ->
                ConeTupleType(elements, type.attributes)
            }
            is ConeVArrayType -> substituteOrNull(type.elementType)?.let { elementType ->
                ConeVArrayType(elementType, type.size, type.attributes)
            }
            is ConePointerType -> substituteOrNull(type.pointeeType)?.let { pointeeType ->
                ConePointerType(pointeeType, type.attributes)
            }
            is ConeTypeAliasType -> substituteTypeAlias(type)
            is ConeIntersectionType -> substituteTypes(type.intersectedTypes)?.let { intersectedTypes ->
                ConeIntersectionType(intersectedTypes, type.attributes)
            }
            is ConeUnionType -> substituteTypes(type.unionTypes.toList())?.let { unionTypes ->
                ConeUnionType(unionTypes.toSet(), type.attributes)
            }
            is ConeErrorType -> {
                val delegatedType = type.delegatedType?.let { substituteOrNull(it) ?: it }
                val typeArguments = substituteArguments(type.typeArguments)
                if (delegatedType == type.delegatedType && typeArguments == null) null
                else ConeErrorType(
                    diagnostic = type.diagnostic,
                    isUninferredParameter = type.isUninferredParameter,
                    delegatedType = delegatedType,
                    typeArguments = typeArguments ?: type.typeArguments,
                    attributes = type.attributes,
                    nullable = type.nullable,
                )
            }
            else -> null
        }
    }

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        return substituteOrNull(projection.type)?.let(::ConeTypeProjection)
    }

    private fun substituteFunction(type: ConeFuncType): ConeFuncType? {
        val parameterTypes = substituteTypes(type.parameterTypes)
        val returnType = substituteOrNull(type.returnType)
        if (parameterTypes == null && returnType == null) return null
        return ConeFuncType(
            parameterTypes = parameterTypes ?: type.parameterTypes,
            returnType = returnType ?: type.returnType,
            isCFunc = type.isCFunc,
            isClosureType = type.isClosureType,
            hasVariableLenArg = type.hasVariableLenArg,
            attributes = type.attributes,
        )
    }

    private fun substituteTypeAlias(type: ConeTypeAliasType): ConeTypeAliasType? {
        val expandedType = type.expandedType?.let { substituteOrNull(it) ?: it }
        val typeArguments = substituteArguments(type.typeArguments)
        if (expandedType == type.expandedType && typeArguments == null) return null
        return ConeTypeAliasType(
            classId = type.classId,
            expandedType = expandedType,
            typeArguments = typeArguments ?: type.typeArguments,
            attributes = type.attributes,
        )
    }

    private fun substituteArguments(arguments: List<ConeTypeProjection>): List<ConeTypeProjection>? {
        var changed = false
        val substituted = arguments.mapIndexed { index, projection ->
            substituteArgument(projection, index)?.also { changed = true } ?: projection
        }
        return substituted.takeIf { changed }
    }

    private fun substituteTypes(types: List<ConeCangJieType>): List<ConeCangJieType>? {
        var changed = false
        val substituted = types.map { type ->
            substituteOrNull(type)?.also { changed = true } ?: type
        }
        return substituted.takeIf { changed }
    }
}
