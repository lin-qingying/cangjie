package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
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
        return withArguments(newArguments)
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

private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType {
    return when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        else -> this
    }
}
