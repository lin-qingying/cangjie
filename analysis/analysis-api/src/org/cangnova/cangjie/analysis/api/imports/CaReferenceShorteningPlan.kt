package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

interface CaReferenceShorteningPlan : CaLifetimeOwner {
    val file: CjFile
    val operations: List<CaReferenceShorteningOperation>
}
