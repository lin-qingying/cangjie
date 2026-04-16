package org.cangnova.cangjie.analysis.api.lightDeclarations

import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.name.CallableId

interface CaLightCallableDeclaration : CaLightDeclaration {
    val callableId: CallableId?

    val signature: CaSignature?
}
