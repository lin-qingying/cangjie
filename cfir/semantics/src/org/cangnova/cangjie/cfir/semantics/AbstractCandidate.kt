package org.cangnova.cangjie.cfir.semantics

import org.cangnova.cangjie.cfir.symbols.CfirSymbol

/**
 * Base candidate abstraction for cone diagnostics.
 */
abstract class AbstractCandidate {
    abstract val symbol: CfirSymbol<*>
    abstract val applicability: CandidateApplicability
}
