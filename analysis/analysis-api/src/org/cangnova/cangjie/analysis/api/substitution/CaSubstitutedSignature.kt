package org.cangnova.cangjie.analysis.api.substitution

import org.cangnova.cangjie.analysis.api.signatures.CaSignature

interface CaSubstitutedSignature : CaSignature {
    val substitutor: CaTypeSubstitutor

    val original: CaSignature
}
