package org.cangnova.cangjie.analysis.api.imports

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

interface CaReferenceShorteningCommand : CaLifetimeOwner {
    val file: CjFile
    val selection: TextRange
    val operations: List<CaReferenceShorteningOperation>
    val importsToAdd: Set<ImportPath>

    val isEmpty: Boolean
        get() = operations.isEmpty()
}
