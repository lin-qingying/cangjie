package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.type.model.CaptureStatus
import org.cangnova.cangjie.type.model.CapturedTypeConstructorMarker
import org.cangnova.cangjie.type.model.CapturedTypeMarker
import org.cangnova.cangjie.type.model.IntersectionTypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker
import kotlin.hashCode

sealed interface ConeTypeConstructorMarker : TypeConstructorMarker


class ConeCapturedTypeConstructor(
    val projection: ConeTypeProjection,
    val lowerType: ConeCangJieType?,
    val captureStatus: CaptureStatus,
    var supertypes: List<ConeCangJieType>? = null,
    val typeParameterMarker: TypeParameterMarker? = null
) : CapturedTypeConstructorMarker, ConeTypeConstructorMarker

data class ConeCapturedType(

    val constructor: ConeCapturedTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleCangJieType(), CapturedTypeMarker {

    override val typeArguments: List<ConeTypeProjection>
        get() = emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeCapturedType

        if (constructor != other.constructor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 7
        result = 31 * result + constructor.hashCode()
        return result
    }
}

data class ConeStubTypeConstructor(
    val variable:  ConeTypeVariable,
    val isTypeVariableInSubtyping: Boolean,
    val isForFixation: Boolean = false,
) : ConeTypeConstructorMarker {
    override fun toString(): String {
        return "Stub(${variable.typeConstructor.debugName})"
    }
}