/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.visitors.transformSingle

internal sealed class LLCfirAbstractBodyTargetResolver(
    resolveTarget: LLCfirResolveTarget,
    resolvePhase: CfirResolvePhase,
    protected val llImplicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession = LLImplicitBodyResolveComputationSession(),
) : LLCfirTargetResolver(resolveTarget, resolvePhase) {
    protected fun createReturnTypeCalculator(): ReturnTypeCalculatorWithJump = ReturnTypeCalculatorWithJump(
        resolveTargetSession,
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
    override fun withContainingFile(cfirFile: CfirFile, action: () -> Unit) {
        transformer.declarationsTransformer.context.withFile(cfirFile) {
            action()
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        val declarationsTransformer = transformer.declarationsTransformer
        val context = declarationsTransformer.context
        val actionWithScopes = {
            context.withScopesForClass(cfirClassLike, declarationsTransformer.components) {
                action()
            }
        }

        if (cfirClassLike is CfirClass) {
            context.withContainingClass(cfirClassLike, actionWithScopes)
        } else {
            actionWithScopes()
        }
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        transformer.declarationsTransformer.context.withContainer(cfirExtend) {
            action()
        }
    }

    protected fun <T : CfirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, CfirDesignation>) {
        val cfirDesignation = CfirDesignation(containingDeclarations, target)
        resolveWithKeeper(target, cfirDesignation, keeper, { CfirLazyBodiesCalculator.calculateBodies(cfirDesignation) }) {
            rawResolve(target)
        }
    }

    protected open fun rawResolve(target: CfirElementWithResolveState) {
        target.transformSingle(transformer, ResolutionMode.ContextIndependent)
    }

}
