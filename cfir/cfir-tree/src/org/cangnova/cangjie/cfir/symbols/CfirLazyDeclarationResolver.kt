package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * Lazy resolve [CfirBasedSymbol] to [CfirResolvePhase].
 *
 * In the case of lazy resolution (inside Analysis API), it checks that the declaration phase `>= toPhase`.
 * If not, it resolves the declaration for the requested phase.
 *
 * If the [lazyResolveToPhase] is called inside a fir transformer,
 * it should always request the phase which is strictly lower than the current transformer phase, otherwise a deadlock/StackOverflow is possible.
 *
 * For the compiler mode, it does nothing, as the compiler is non-lazy.
 *
 * @receiver [CfirBasedSymbol] which should be resolved
 * @param toPhase the minimum phase, the declaration should be resolved to after an execution of the [lazyResolveToPhase]
 */
fun CfirSymbol<*>.lazyResolveToPhase(toPhase: CfirResolvePhase) {
    cfir.lazyResolveToPhase(toPhase)
}

/**
 * Lazy resolve [CfirElementWithResolveState] to [CfirResolvePhase].
 *
 * @see lazyResolveToPhase
 */
fun CfirElementWithResolveState.lazyResolveToPhase(toPhase: CfirResolvePhase) {
    invokeLazyResolveToPhase(toPhase, CfirLazyDeclarationResolver::lazyResolveToPhase)
}

private fun CfirElementWithResolveState.invokeLazyResolveToPhase(
    toPhase: CfirResolvePhase,
    resolver: CfirLazyDeclarationResolver.(CfirElementWithResolveState, CfirResolvePhase) -> Unit,
) {
    when (this) {


        else -> lazyDeclarationResolver.resolver(this, toPhase)
    }
}

private val CfirElementWithResolveState.lazyDeclarationResolver get() = moduleData.session.lazyDeclarationResolver
val CfirSession.lazyDeclarationResolver: CfirLazyDeclarationResolver by CfirSession.sessionComponentAccessor()

/**
 * Tracks active resolving phases for a single CfirSession.
 */
abstract class CfirLazyDeclarationResolver : CfirSessionComponent {
    private val phaseStack: ArrayDeque<CfirResolvePhase> = ArrayDeque()

    fun startResolvingPhase(phase: CfirResolvePhase) {
        phaseStack.addLast(phase)
    }
    abstract fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase)

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