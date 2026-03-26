package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection

sealed class TypeArgumentMapping {
    abstract operator fun get(typeParameterIndex: Int): ConeTypeProjection

    data object NoExplicitArguments : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): ConeTypeProjection = buildPlaceholderProjection()
    }

    class Mapped(
        private val ordered: List<ConeTypeProjection>,
    ) : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): ConeTypeProjection {
            return ordered.getOrElse(typeParameterIndex) { buildPlaceholderProjection() }
        }
    }
}

private fun buildPlaceholderProjection(): ConeTypeProjection {
    return ConeTypeProjection(ConePlaceholderType())
}
