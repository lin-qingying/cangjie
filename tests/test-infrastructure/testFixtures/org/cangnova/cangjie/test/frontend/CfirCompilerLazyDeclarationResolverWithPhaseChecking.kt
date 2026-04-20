package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.isItAllowedToCallLazyResolveTo
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.util.PrivateForInline

class CfirCompilerLazyDeclarationResolverWithPhaseChecking : CfirLazyDeclarationResolver() {
    private var currentTransformerPhase: CfirResolvePhase? = null

    private val exceptions = mutableListOf<CfirLazyResolveContractViolationException>()

    fun getContractViolationExceptions(): List<CfirLazyResolveContractViolationException> = exceptions

    override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, element.resolvePhase)
    }

    override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, clazz.resolvePhase)
    }

    override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        checkIfCanLazyResolveToPhase(toPhase, element.resolvePhase)
    }

    override fun startResolvingPhase(phase: CfirResolvePhase) {
        check(currentTransformerPhase == null)
        currentTransformerPhase = phase
    }

    override fun finishResolvingPhase(phase: CfirResolvePhase) {
        check(currentTransformerPhase == phase)
        currentTransformerPhase = null
    }

    @OptIn(PrivateForInline::class)
    private fun checkIfCanLazyResolveToPhase(requestedPhase: CfirResolvePhase, elementPhase: CfirResolvePhase) {
        if (!_lazyResolveContractChecksEnabled.get() || elementPhase >= requestedPhase) return

        val currentPhase = currentTransformerPhase
            ?: error("Current phase is not set, please call ${this::startResolvingPhase.name} before starting transforming the file")

        if (!currentPhase.isItAllowedToCallLazyResolveTo(requestedPhase)) {
            exceptions += CfirLazyResolveContractViolationException(
                currentPhase = currentPhase,
                requestedPhase = requestedPhase,
            )
        }
    }
}

class CfirLazyResolveContractViolationException(
    val currentPhase: CfirResolvePhase,
    val requestedPhase: CfirResolvePhase,
) : IllegalStateException("Lazy resolve contract violated: current=$currentPhase requested=$requestedPhase")
