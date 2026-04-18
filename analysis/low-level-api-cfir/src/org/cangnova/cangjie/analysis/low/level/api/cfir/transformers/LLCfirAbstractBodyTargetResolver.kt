/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.LLCfirReturnTypeCalculatorWithJump
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.visitors.transformSingle

internal sealed class LLCfirAbstractBodyTargetResolver(
    resolveTarget: LLCfirResolveTarget,
    resolvePhase: CfirResolvePhase,
    protected val llImplicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession = LLImplicitBodyResolveComputationSession(),
) : LLCfirTargetResolver(resolveTarget, resolvePhase) {
    protected fun createReturnTypeCalculator(): LLCfirReturnTypeCalculatorWithJump = LLCfirReturnTypeCalculatorWithJump(
        resolveTargetScopeSession,
        llImplicitBodyResolveComputationSession,
    )

    abstract val transformer: CfirAbstractBodyResolveTransformerDispatcher

    override fun checkResolveConsistency() {
        check(resolverPhase == transformer.transformerPhase) {
            "Inconsistent Resolver($resolverPhase) and Transformer(${transformer.transformerPhase}) phases"
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(firFile: CfirFile, action: () -> Unit) {
        transformer.declarationsTransformer?.withFile(firFile) {
            action()
            firFile
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withRegularClass", level = DeprecationLevel.ERROR)
    override fun withContainingRegularClass(firClass: CfirRegularClass, action: () -> Unit) {
        transformer.declarationsTransformer?.context?.withContainingClass(firClass) {
            transformer.declarationsTransformer?.forRegularClassBody(firClass) {
                action()
                firClass
            }
        }
    }

    protected fun <T : CfirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, CfirDesignation>) {
        val firDesignation = CfirDesignation(containingDeclarations, target)
        resolveWithKeeper(target, firDesignation, keeper, { CfirLazyBodiesCalculator.calculateBodies(firDesignation) }) {
            rawResolve(target)
        }
    }

    protected open fun rawResolve(target: CfirElementWithResolveState) {
        target.transformSingle(transformer, ResolutionMode.ContextIndependent)
    }

}
