package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability

/**
 * Base candidate abstraction for cone diagnostics.
 */
abstract class AbstractCandidate {
    abstract val symbol: CfirBasedSymbol<*>
    abstract val applicability: CandidateApplicability
}
