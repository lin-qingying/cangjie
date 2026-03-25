package org.cangnova.cangjie.cfir.types

/**
 * Stable placeholder type used when the call pipeline needs to keep an explicit
 * slot for a type argument before a real type is available.
 */
class ConePlaceholderType(
    val debugName: String = "_",
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleCangJieType(), ConeTypeConstructorMarker {
    override val typeArguments: List<ConeTypeProjection>
        get() = emptyList()

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun toString(): String = debugName
}
