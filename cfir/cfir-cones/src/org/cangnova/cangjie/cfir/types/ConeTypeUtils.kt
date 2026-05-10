package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.render.ConeTypeRendererForDebugging
import org.cangnova.cangjie.utils.SmartSet
import org.cangnova.cangjie.utils.popLast


fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean): Boolean {
    return contains(predicate, SmartSet.create())
}
/**
 * Recursively visits each [ConeCangJieType] inside (including itself) and performs the given action.
 * Doesn't give guarantees on the traversal order.
 */
inline fun ConeCangJieType.forEachType(
    prepareType: (ConeCangJieType) -> ConeCangJieType = { it },
    action: (ConeCangJieType) -> Unit,
) {
    val stack = mutableListOf(this)

    while (stack.isNotEmpty()) {
        val next = stack.popLast().let(prepareType)
        action(next)

        when (next) {
            is ConeFunctionType -> {
                stack.add(next.returnType)
                stack.addAll(next.parameterTypes)
            }
            is ConeTupleType -> stack.addAll(next.elementTypes)
            is ConeVArrayType -> stack.add(next.elementType)
            is ConePointerType -> stack.add(next.pointeeType)
            is ConeIntersectionType -> stack.addAll(next.intersectedTypes)
            is ConeUnionType -> stack.addAll(next.unionTypes)
            else -> next.typeArguments.forEach { projection -> stack.add(projection.type) }
        }
    }
}

private fun ConeCangJieType.contains(predicate: (ConeCangJieType) -> Boolean, visited: SmartSet<ConeCangJieType>): Boolean {
    if (this in visited) return false
    if (predicate(this)) return true
    visited += this

    return when (this) {
        is ConeFunctionType -> parameterTypes.any { it.contains(predicate, visited) } || returnType.contains(predicate, visited)
        is ConeTupleType -> elementTypes.any { it.contains(predicate, visited) }
        is ConeVArrayType -> elementType.contains(predicate, visited)
        is ConePointerType -> pointeeType.contains(predicate, visited)
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        is ConeUnionType -> unionTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { projection -> projection.type.contains(predicate, visited) }
    }
}

fun ConeCangJieType.renderForDebugging(): String {
    val builder = StringBuilder()
    ConeTypeRendererForDebugging(builder).render(this)
    return builder.toString()
}

fun ConeIntersectionType.withUpperBound(upperBound: ConeCangJieType): ConeIntersectionType {
    return ConeIntersectionType(
        intersectedTypes = intersectedTypes,
        upperBoundForApproximation = upperBound,
        attributes = attributes,
    )
}

fun ConeRigidType.getConstructor(): ConeTypeConstructorMarker {
    return when (this) {
        is ConeLookupTagBasedType -> this.lookupTag
        is ConeTypeVariableType -> this.typeConstructor
        is ConeStubType -> this.constructor
        is ConeTypeConstructorMarker -> this
    }
}
