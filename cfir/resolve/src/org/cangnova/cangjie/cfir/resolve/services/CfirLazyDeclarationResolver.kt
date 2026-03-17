package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * Tracks active resolving phases for a single CfirSession.
 */
class CfirLazyDeclarationResolver : CfirSessionComponent {
    private val phaseStack: ArrayDeque<CfirResolvePhase> = ArrayDeque()

    fun startResolvingPhase(phase: CfirResolvePhase) {
        phaseStack.addLast(phase)
    }

    fun finishResolvingPhase(phase: CfirResolvePhase) {
        check(phaseStack.isNotEmpty()) { "No active resolve phase when finishing $phase" }
        val current = phaseStack.removeLast()
        check(current == phase) { "Mismatched resolve phase: expected finish $current, got $phase" }
    }

    val currentPhase: CfirResolvePhase?
        get() = phaseStack.lastOrNull()

    fun isResolving(phase: CfirResolvePhase): Boolean = currentPhase == phase

    val isResolvingAny: Boolean
        get() = phaseStack.isNotEmpty()
}

