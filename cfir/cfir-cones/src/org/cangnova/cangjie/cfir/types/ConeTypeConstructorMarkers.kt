package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.type.model.IntersectionTypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

sealed interface ConeTypeConstructorMarker : TypeConstructorMarker


data class ConeStubTypeConstructor(
    val variable:  ConeTypeVariable,
    val isTypeVariableInSubtyping: Boolean,
    val isForFixation: Boolean = false,
) : ConeTypeConstructorMarker {
    override fun toString(): String {
        return "Stub(${variable.typeConstructor.debugName})"
    }
}
