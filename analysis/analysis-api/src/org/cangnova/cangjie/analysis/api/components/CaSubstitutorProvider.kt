package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

interface CaSubstitutorProvider : CaLifetimeOwner {
    fun createTypeSubstitutor(substitutions: Map<Name, CaType>): CaTypeSubstitutor

    fun CaSignature.createSubstitutor(typeArguments: List<CaType>): CaTypeSubstitutor
}
