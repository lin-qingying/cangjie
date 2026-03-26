package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.utils.SmartSet


fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean): Boolean {
    return contains(predicate, SmartSet.create())
}
private fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean, visited: SmartSet<ConeCangJieType>): Boolean {
    if (this in visited) return false
    if (predicate(this)) return true
    visited += this

    return when (this) {
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { it is  ConeTypeProjection && it.type.contains(predicate, visited) }
    }
}

fun ConeCangJieType.renderForDebugging(): String {
    val builder = StringBuilder()
    ConeTypeRendererForDebugging(builder).render(this)
    return builder.toString()
}

fun ConeRigidType.getConstructor(): ConeTypeConstructorMarker {
    return when (this) {
        is ConeLookupTagBasedType -> this.lookupTag
        is ConeTypeVariableType -> this.typeConstructor
        is ConeStubType -> this.constructor
        is ConeTypeConstructorMarker -> this
    }
}
