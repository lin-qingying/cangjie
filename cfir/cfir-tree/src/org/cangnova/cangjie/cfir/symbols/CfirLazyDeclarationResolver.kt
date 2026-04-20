package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.util.PrivateForInline

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
fun CfirBasedSymbol<*>.lazyResolveToPhase(toPhase: CfirResolvePhase) {
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

fun CfirClass.lazyResolveToPhaseWithCallableMembers(toPhase: CfirResolvePhase) {
    invokeLazyResolveToPhase(toPhase, CfirLazyDeclarationResolver::lazyResolveToPhaseWithCallableMembers)
}

fun CfirElementWithResolveState.lazyResolveToPhaseRecursively(toPhase: CfirResolvePhase) {
    invokeLazyResolveToPhase(toPhase, CfirLazyDeclarationResolver::lazyResolveToPhaseRecursively)
}

private fun CfirElementWithResolveState.invokeLazyResolveToPhase(
    toPhase: CfirResolvePhase,
    resolver: CfirLazyDeclarationResolver.(CfirElementWithResolveState, CfirResolvePhase) -> Unit,
) {
    when (this) {


        else -> lazyDeclarationResolver.resolver(this, toPhase)
    }
}

private fun CfirClass.invokeLazyResolveToPhase(
    toPhase: CfirResolvePhase,
    resolver: CfirLazyDeclarationResolver.(CfirClass, CfirResolvePhase) -> Unit,
) {
    lazyDeclarationResolver.resolver(this, toPhase)
}

private val CfirElementWithResolveState.lazyDeclarationResolver get() = moduleData.session.lazyDeclarationResolver
val CfirSession.lazyDeclarationResolver: CfirLazyDeclarationResolver by CfirSession.sessionComponentAccessor()

/**
 * Tracks active resolving phases for a single CfirSession.
 */
abstract class CfirLazyDeclarationResolver : CfirSessionComponent {
    @PrivateForInline
    @Suppress("PropertyName")
    val _lazyResolveContractChecksEnabled: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }
    abstract fun startResolvingPhase(phase: CfirResolvePhase)

    abstract fun finishResolvingPhase(phase: CfirResolvePhase)


    @PrivateForInline
    @Suppress("PropertyName")
    val _lazyResolveIsAllowed: ThreadLocal<Boolean> = ThreadLocal.withInitial { true }

    abstract fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase)

    open fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {
        lazyResolveToPhase(clazz, toPhase)
    }

    open fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        lazyResolveToPhase(element, toPhase)
    }
    @OptIn(PrivateForInline::class)
    inline fun <T> forbidLazyResolveInside(action: () -> T): T {
        val current = _lazyResolveIsAllowed.get()
        _lazyResolveIsAllowed.set(false)
        try {
            return action()
        } finally {
            _lazyResolveIsAllowed.set(current)
        }
    }

    @OptIn(PrivateForInline::class)
    protected fun assertLazyResolveAllowed() {
        if (!_lazyResolveIsAllowed.get()) {
            throw CfirLazyResolveForbiddenException()
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> disableLazyResolveContractChecksInside(action: () -> T): T {
        val current = _lazyResolveContractChecksEnabled.get()
        _lazyResolveContractChecksEnabled.set(false)
        try {
            return action()
        } finally {
            _lazyResolveContractChecksEnabled.set(current)
        }
    }


}
class CfirLazyResolveForbiddenException() : IllegalStateException("Lazy resolve is forbidden")

object CfirDummyCompilerLazyDeclarationResolver : CfirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: CfirResolvePhase) {}
    override fun finishResolvingPhase(phase: CfirResolvePhase) {}
    override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {}
    override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {}
    override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {}
}


