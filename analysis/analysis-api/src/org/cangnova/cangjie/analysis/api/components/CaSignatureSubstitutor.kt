package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor

interface CaSignatureSubstitutor : CaLifetimeOwner {
    fun CaSignature.substitute(substitutor: CaTypeSubstitutor): CaSubstitutedSignature
}
