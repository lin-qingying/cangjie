package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
public interface CaAnalysisScopeProvider : CaSessionComponent {
    /**
     * A [GlobalSearchScope] which spans the files that can be analyzed by the current [KaSession].
     *
     * For example, [KaSymbol]s can only be built for declarations which are in the analysis scope.
     */
    public val analysisScope: GlobalSearchScope

    /**
     * Checks whether the [PsiElement] is inside the [analysisScope].
     *
     * For example, a [KaSymbol] can only be built for this [PsiElement] if it can be analyzed.
     */
    public fun PsiElement.canBeAnalysed(): Boolean
}

context(session: CaSession)
public val analysisScope: GlobalSearchScope
    get() = with(session) { analysisScope }
