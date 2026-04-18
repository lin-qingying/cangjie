/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirPartialBodyResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkPhase
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * This class is responsible for [LLCfirResolveTarget] resolution and "is resolved" check after that.
 *
 * @see LLCfirLazyResolverRunner
 * @see LLCfirTargetResolver
 */
internal sealed class LLCfirLazyResolver(val resolverPhase: CfirResolvePhase) {
    fun resolve(target: LLCfirResolveTarget) {
        val resolver = createTargetResolver(target)
        requireWithAttachment(
            resolverPhase == resolver.resolverPhase,
            {
                """
                Phase mismatch between ${this::class.simpleName} and ${resolver::class.simpleName}.
                The resolver phase is ${resolver.resolverPhase}, but $resolverPhase is expected
                """.trimIndent()
            },
        )

        resolver.resolveDesignation()

        if (target !is LLCfirPartialBodyResolveTarget) {
            target.forEachTarget(::checkIsResolved)
        }

        checkCanceled()
    }

    protected abstract fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver

    fun checkIsResolved(target: CfirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        phaseSpecificCheckIsResolved(target)
        checkNestedDeclarationsAreResolved(target)
    }

    /**
     * Check that phase-specific conditions are met
     * Will be performed to resolved declaration and its nested declarations
     * @see checkNestedDeclarationsAreResolved
     */
    protected abstract fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState)

    private fun checkNestedDeclarationsAreResolved(target: CfirElementWithResolveState) {
        if (target !is CfirDeclaration) return

        checkFunctionParametersAreResolved(target)
        checkVariableSubDeclarationsAreResolved(target)
        checkTypeParametersAreResolved(target)
        checkReceiversAreResolved(target)
    }

    private fun checkReceiversAreResolved(declaration: CfirDeclaration) {
        when (declaration) {
            is CfirCallableDeclaration -> {
                declaration.receiverParameter?.let(::checkIsResolved)
            }

            else -> {}
        }
    }

    private fun checkVariableSubDeclarationsAreResolved(declaration: CfirDeclaration) {
        if (declaration !is CfirVariable) return

        declaration.getter?.let(::checkIsResolved)
        declaration.setter?.let(::checkIsResolved)
        declaration.backingField?.let(::checkIsResolved)
    }

    private fun checkFunctionParametersAreResolved(declaration: CfirDeclaration) {
        if (declaration !is CfirFunction) return

        declaration.valueParameters.forEach(::checkIsResolved)
    }

    private fun checkTypeParametersAreResolved(declaration: CfirDeclaration) {
        if (declaration !is CfirTypeParameterRefsOwner) return

        for (parameter in declaration.typeParameters) {
            if (parameter !is CfirTypeParameter) continue
            checkIsResolved(parameter)
        }
    }
}
