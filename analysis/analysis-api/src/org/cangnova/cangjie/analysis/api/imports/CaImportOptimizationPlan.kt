package org.cangnova.cangjie.analysis.api.imports

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportInfo

interface CaImportOptimizationPlan : CaLifetimeOwner {
    val file: CjFile
    val retainedImports: List<CjImportInfo>
    val duplicateImports: List<CjImportInfo>
    val unusedImports: List<CjImportInfo>
    val missingImports: List<ImportPath>
}
