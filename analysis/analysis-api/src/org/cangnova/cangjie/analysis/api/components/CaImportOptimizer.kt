package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.psi.CjFile

interface CaImportOptimizer : CaLifetimeOwner {
    fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan
}
