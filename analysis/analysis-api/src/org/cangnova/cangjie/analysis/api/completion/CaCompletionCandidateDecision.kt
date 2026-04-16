package org.cangnova.cangjie.analysis.api.completion

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

interface CaCompletionCandidateDecision : CaLifetimeOwner {
    val symbol: CaSymbol

    val status: CaCompletionCandidateStatus

    val requiredImport: ImportPath?
}
