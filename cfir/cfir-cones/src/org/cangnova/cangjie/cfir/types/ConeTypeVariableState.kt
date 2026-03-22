package org.cangnova.cangjie.cfir.types

data class ConeTypeVariableId(
    val name: String,
    val freshId: Int,
)

data class ConeTypeVariableState(
    val id: ConeTypeVariableId,
    val upperBounds: MutableList<ConeCangJieType> = mutableListOf(),
    val lowerBounds: MutableList<ConeCangJieType> = mutableListOf(),
) {
    var fixedType: ConeCangJieType? = null
}

class ConeTypeVariableRef(
    val state: ConeTypeVariableState,
    override val attributes: ConeAttributes = ConeAttributes.EMPTY,
) : ConeRigidType() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeTypeVariableRef) return false
        return state.id == other.state.id
    }

    override fun hashCode(): Int = state.id.hashCode()

    override fun toString(): String = state.id.name
}
