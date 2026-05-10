package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeSubstitutorMarker

abstract class AbstractConeSubstitutor(
    protected val typeContext: ConeTypeContext,
) : ConeSubstitutor() {
    abstract fun substituteType(type: ConeCangJieType): ConeCangJieType?

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? {
        val newType = substituteOrNull(projection.type) ?: return null
        return newType
    }

    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? {
        val substitutedType = substituteType(type) ?: type.substituteRecursive()
        val substitutedAttributes = type.attributes.transformTypesWith(this::substituteOrNull)

        return when {
            substitutedType != null && substitutedAttributes != null -> {
                substitutedType.withAttributes(substitutedAttributes)
            }
            substitutedType != null -> substitutedType
            substitutedAttributes != null -> type.withAttributes(substitutedAttributes)
            else -> null
        }
    }

    private fun ConeCangJieType.substituteRecursive(): ConeCangJieType? {
        return when (this) {
            is ConeTypeAliasType -> substituteTypeAlias()
            is ConeRigidType -> substituteArguments()
            else -> null
        }
    }

    private fun ConeTypeAliasType.substituteTypeAlias(): ConeCangJieType? {
        val substitutedExpandedType = substituteOrNull(expandedType)
        val substitutedArguments = substituteArguments()
        if (substitutedExpandedType == null && substitutedArguments == null) return null

        val arguments = (substitutedArguments as? ConeTypeAliasType)?.typeArguments ?: typeArguments
        return ConeTypeAliasType(classId, substitutedExpandedType ?: expandedType, arguments, attributes)
    }

    private fun ConeRigidType.substituteArguments(): ConeCangJieType? {
        val arguments = typeArguments
        if (arguments.isEmpty()) return null

        var changed = false
        val newArguments = ArrayList<ConeTypeProjection>(arguments.size)

        for ((index, argument) in arguments.withIndex()) {
            val substituted = substituteArgument(argument, index)
            if (substituted != null) {
                changed = true
                newArguments += substituted
            } else {
                newArguments += argument
            }
        }

        if (!changed) return null
        return withReplacedArguments(newArguments)
    }
}

fun createTypeSubstitutorByTypeConstructor(
    map: Map<TypeConstructorMarker, ConeCangJieType>,
    context: ConeTypeContext,
    approximateIntegerLiterals: Boolean,
): ConeSubstitutor {
    if (map.isEmpty()) return ConeSubstitutor.Empty
    return ConeTypeSubstitutorByTypeConstructor(map, context, approximateIntegerLiterals)
}

private class ConeTypeSubstitutorByTypeConstructor(
    private val map: Map<TypeConstructorMarker, ConeCangJieType>,
    typeContext: ConeTypeContext,
    private val approximateIntegerLiterals: Boolean,
) : AbstractConeSubstitutor(typeContext) {

    override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
        val constructor = type.typeConstructorForSubstitution() ?: return null
        val newType = map[constructor] ?: return null
        return if (approximateIntegerLiterals) newType.approximateIntegerLiteralType() else newType
    }

    override fun toString(): String {
        return map.entries.joinToString(prefix = "{", postfix = "}", separator = " | ") { (constructor, type) ->
            "$constructor -> $type"
        }
    }
}

object ConeEmptySubstitutor : ConeSubstitutor() {
    override fun substituteOrSelf(type: ConeCangJieType): ConeCangJieType = type

    override fun substituteOrNull(type: ConeCangJieType): ConeCangJieType? = null

    override fun substituteArgument(projection: ConeTypeProjection, index: Int): ConeTypeProjection? = null

    override fun toString(): String = "ConeEmptySubstitutor"
}

@Suppress("NOTHING_TO_INLINE")
inline fun TypeSubstitutorMarker.asCone(): ConeSubstitutor = this as ConeSubstitutor

@Deprecated(message = "This call is redundant, please just drop it", level = DeprecationLevel.ERROR)
fun ConeSubstitutor.asCone(): ConeSubstitutor = this

fun ConeSubstitutor.substituteOrNull(type: ConeCangJieType?): ConeCangJieType? {
    return type?.let { substituteOrNull(it) }
}

private fun ConeCangJieType.typeConstructorForSubstitution(): TypeConstructorMarker? {
    return when (this) {
        is ConeLookupTagBasedType -> lookupTag
        is ConeTypeVariableType -> typeConstructor
        is ConeStubType -> constructor
        is ConeTypeConstructorMarker -> this
        else -> null
    }
}

private fun ConeCangJieType.withAttributes(newAttributes: ConeAttributes): ConeCangJieType {
    return when (this) {
        is ConeClassLikeType -> ConeClassLikeType(lookupTag, typeArguments, newAttributes, isInterface, isThisType)
        is ConeStructType -> ConeStructType(lookupTag, typeArguments, newAttributes)
        is ConeEnumType -> ConeEnumType(lookupTag, typeArguments, newAttributes, isRefEnum)
        is ConePrimitiveType -> ConePrimitiveType(kind, newAttributes)
        is ConeCStringType -> ConeCStringType(newAttributes)
        is ConeTypeParameterType -> ConeTypeParameterTypeImpl(lookupTag, newAttributes)
        is ConeFunctionType -> ConeFunctionType(
            parameterTypes,
            returnType,
            isCFunc,
            isClosureType,
            hasVariableLenArg,
            newAttributes
        )
        is ConeTupleType -> ConeTupleType(elementTypes, newAttributes)
        is ConeVArrayType -> ConeVArrayType(elementType, size, newAttributes)
        is ConePointerType -> ConePointerType(pointeeType, newAttributes)
        is ConeIntersectionType -> ConeIntersectionType(
            intersectedTypes = intersectedTypes,
            upperBoundForApproximation = upperBoundForApproximation,
            attributes = newAttributes,
        )
        is ConeUnionType -> ConeUnionType(unionTypes, newAttributes)
        is ConeTypeAliasType -> ConeTypeAliasType(classId, expandedType, typeArguments, newAttributes)
        is ConeErrorType -> ConeErrorType(
            diagnostic,
            isUninferredParameter,
            delegatedType,
            typeArguments,
            newAttributes
        )
        is ConeQuestType -> ConeQuestType(newAttributes)
        is ConeTypeVariableType -> ConeTypeVariableType(typeConstructor, newAttributes)
        is ConePlaceholderType -> ConePlaceholderType(debugName, newAttributes)
        else -> this
    }
}

private fun ConeCangJieType.withReplacedArguments(newArguments: List<ConeTypeProjection>): ConeCangJieType {
    return when (this) {
        is ConeClassLikeType -> ConeClassLikeType(lookupTag, newArguments, attributes, isInterface, isThisType)
        is ConeStructType -> ConeStructType(lookupTag, newArguments, attributes)
        is ConeEnumType -> ConeEnumType(lookupTag, newArguments, attributes, isRefEnum)
        is ConeFunctionType -> {
            val paramTypes = newArguments.dropLast(1).map { it.type }
            val retType = newArguments.last().type
            ConeFunctionType(paramTypes, retType, isCFunc, isClosureType, hasVariableLenArg, attributes)
        }
        is ConeTupleType -> ConeTupleType(newArguments.map { it.type }, attributes)
        is ConeVArrayType -> ConeVArrayType(newArguments.first().type, size, attributes)
        is ConePointerType -> ConePointerType(newArguments.first().type, attributes)
        is ConeIntersectionType -> ConeIntersectionType(
            intersectedTypes = newArguments.map { it.type },
            upperBoundForApproximation = upperBoundForApproximation,
            attributes = attributes,
        )
        is ConeUnionType -> ConeUnionType(newArguments.map { it.type }.toSet(), attributes)
        is ConeTypeAliasType -> ConeTypeAliasType(classId, expandedType, newArguments, attributes)
        is ConeErrorType -> ConeErrorType(
            diagnostic,
            isUninferredParameter,
            delegatedType,
            newArguments,
            attributes
        )
        else -> this
    }
}

private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        else -> this
    }
}
