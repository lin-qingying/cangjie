package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

interface CaAnalysisScopeProvider : CaLifetimeOwner {
    fun CaModule.analysisScope(): GlobalSearchScope
}
