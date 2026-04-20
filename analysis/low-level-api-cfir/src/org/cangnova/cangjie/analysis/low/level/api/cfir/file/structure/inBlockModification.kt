/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.psi

/**
 * Must be called in a write action.
 * @return **false** if it is not in-block modification
 */
internal fun invalidateAfterInBlockModification(declaration: CfirDeclaration): Boolean = when (declaration) {
    is CfirNamedFunction -> declaration.inBodyInvalidation()
    is CfirPropertyAccessor -> declaration.inBodyInvalidation()
    is CfirProperty -> declaration.inBodyInvalidation()
    is CfirCodeFragment -> declaration.inBodyInvalidation()
    else -> errorWithCfirSpecificEntries("Unknown declaration with body", fir = declaration, psi = declaration.psi)
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * body
 * * control flow graph reference, because it depends on the body
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * Also, we shouldn't update somehow value parameters because they have their own "bodies" (a default value) and
 * changes in them are OOBM, so it is not our case.
 *
 * @return **false** if it is an out-of-block change
 */
private fun CfirNamedFunction.inBodyInvalidation(): Boolean {
    val body = body ?: return false
    invalidateBody(body)
    return true
}

private fun CfirFunction.invalidateBody(body: CfirBlock): CfirResolvePhase? {
    // the body is not yet resolved, so there is nothing to invalidate
    if (body is CfirLazyBlock) return null
    val newPhase = phaseWithoutBody

    decreasePhase(newPhase)
    replaceBody(buildLazyBlock())
    replaceControlFlowGraphReference(newControlFlowGraphReference = null)

    return newPhase
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * initializer expression
 * * control flow graph reference, because it depends on the initializer
 * * body resolution state
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * Also, we shouldn't update the property accessors because they don't depend on the initializer.
 * So it is fine to leave the phase of setter/getter/backing field as it is.
 *
 * @return **false** if it is an out-of-block change
 */
private fun CfirProperty.inBodyInvalidation(): Boolean {
    val getterBody = getter?.body
    val setterBody = setter?.body
    if (getterBody == null && setterBody == null) {
        return false
    }
    if (getterBody is CfirLazyBlock || setterBody is CfirLazyBlock) {
        return true
    }

    decreasePhase(phaseWithoutBody)
    replaceControlFlowGraphReference(null)
    replaceBodyResolveState(CfirPropertyBodyResolveState.NOTHING_RESOLVED)

    return true
}

/**
 * Drop body and all related stuff.
 * We should drop:
 * * body
 * * control flow graph reference, because it depends on the body
 * * property body resolution state
 * * reduce phase if needed
 *
 * Depends on the body, but we shouldn't drop:
 * * implicit type, because the change mustn't change the resulting type
 *
 * @return **false** if it is an out-of-block change
 */
private fun CfirPropertyAccessor.inBodyInvalidation(): Boolean {
    val body = body ?: return false
    val newPhase = invalidateBody(body) ?: return true

    val property = propertySymbol.cfir
    property.decreasePhase(newPhase)

    val newPropertyResolveState = if (isGetter) {
        CfirPropertyBodyResolveState.INITIALIZER_RESOLVED
    } else {
        CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
    }

    property.replaceBodyResolveState(minOf(property.bodyResolveState, newPropertyResolveState))
    return true
}

private fun CfirCodeFragment.inBodyInvalidation(): Boolean {
    if (block is CfirLazyBlock) {
        return true
    }

    decreasePhase(CfirResolvePhase.BODY_RESOLVE.previous)
    replaceBlock(buildLazyBlock())

    return true
}

private val CfirDeclaration.phaseWithoutBody: CfirResolvePhase
    get() {
        return minOf(CfirResolvePhase.BODY_RESOLVE.previous, resolvePhase)
    }

private fun CfirDeclaration.decreasePhase(newPhase: CfirResolvePhase) {
    if (isPartialBodyResolvable) {
        val oldPhase = resolvePhase
        if (oldPhase >= CfirResolvePhase.BODY_RESOLVE.previous) {
            partialBodyAnalysisState = null
        }
    }

    @OptIn(ResolveStateAccess::class)
    resolveState = newPhase.asResolveState()
}
