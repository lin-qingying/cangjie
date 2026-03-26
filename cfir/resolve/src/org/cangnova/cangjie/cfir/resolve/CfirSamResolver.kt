package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * SAM conversion metadata owner for call resolution.
 *
 * The current CFIR pipeline only needs the conversion payload shape on the
 * candidate/completion path, but the declaration lives here so the dependency
 * surface matches the Kotlin body-resolve architecture instead of being hidden
 * as a local stub inside [org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate].
 */
class CfirSamResolver(
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
) : SessionAndScopeSessionHolder {
    data class SamConversionInfo(
        val functionalType: ConeCangJieType,
        val samType: ConeCangJieType,
    )
}
