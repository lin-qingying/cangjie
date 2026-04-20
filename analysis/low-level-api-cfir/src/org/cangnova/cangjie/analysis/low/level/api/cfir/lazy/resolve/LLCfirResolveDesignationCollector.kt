/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirClassWithAllCallablesResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirPartialBodyResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirWholeElementResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyResolveRequest
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.tryCollectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirResolveDesignationCollector.shouldBeResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.*

/**
 * Collects [LLCfirResolveTarget] for requested [CfirElementWithResolveState].
 *
 * Effectively, this class is responsible for which elements can be lazily resolved and which cannot.
 *
 * @see LLCfirResolveTarget
 * @see shouldBeResolved
 */
internal object LLCfirResolveDesignationCollector {
    fun getDesignationToResolve(target: CfirElementWithResolveState): LLCfirResolveTarget? {
        return getDesignationToResolve(target, CfirDesignation::asResolveTarget)
    }

    fun getDesignationToResolveWithCallableMembers(target: CfirClass): LLCfirResolveTarget? {
        return getDesignationToResolve(target, ::LLCfirClassWithAllCallablesResolveTarget)
    }

    fun getDesignationToResolveRecursively(target: CfirElementWithResolveState): LLCfirResolveTarget? {
        return getDesignationToResolve(target, ::LLCfirWholeElementResolveTarget)
    }

    fun getDesignationToResolveForPartialBody(request: LLPartialBodyResolveRequest): LLCfirResolveTarget? {
        return getDesignationToResolve(request.target) {
            LLCfirPartialBodyResolveTarget(it, request)
        }
    }

    private fun getDesignationToResolve(
        target: CfirElementWithResolveState,
        resolveTarget: (CfirDesignation) -> LLCfirResolveTarget,
    ): LLCfirResolveTarget? {
        val designation = getCfirDesignationToResolve(target) ?: return null
        val llResolveTarget = resolveTarget(designation)
        return llResolveTarget
    }

    private fun getCfirDesignationToResolve(target: CfirElementWithResolveState): CfirDesignation? {
        if (!target.shouldBeResolved()) {
            return null
        }

        return when (target) {
            is CfirPropertyAccessor -> getCfirDesignationToResolve(target.propertySymbol.cfir)
            is CfirTypeParameter -> getCfirDesignationToResolve(target.containingDeclarationSymbol.cfir)
            is CfirValueParameter -> getCfirDesignationToResolve(target.containingDeclarationSymbol.cfir)
            is CfirCallableDeclaration if target.canHaveDeferredReturnTypeCalculation -> CfirDesignation(target)
            else -> target.tryCollectDesignation()
        }
    }

    /**
     * @see isLazyResolvable
     */
    private fun CfirElementWithResolveState.shouldBeResolved() = when (this) {
        is CfirDeclaration -> shouldBeResolved()
        else -> throwUnexpectedCfirElementError(this)
    }

    private fun CfirDeclaration.shouldBeResolved(): Boolean {
        if (!origin.isLazyResolvable) {
            @OptIn(ResolveStateAccess::class)
            check(resolvePhase == CfirResolvePhase.BODY_RESOLVE) {
                "Expected body resolve phase for origin $origin but found $resolveState"
            }

            return false
        }

        return true
    }
}
