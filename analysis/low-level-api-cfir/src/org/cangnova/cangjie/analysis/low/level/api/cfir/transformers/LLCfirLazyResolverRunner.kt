/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirGlobalResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.session
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

/**
 * Runs required [LLCfirLazyResolver] on [LLCfirResolveTarget] based on [CfirResolvePhase].
 *
 * @see runLazyResolverByPhase
 * @see LLCfirLazyResolver
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirLockProvider
 */
internal object LLCfirLazyResolverRunner {
    /**
     * Runs [resolver][LLCfirLazyResolver] associated with [phase] for [target].
     *
     * @see LLCfirLazyPhaseResolverByPhase
     */
    fun runLazyResolverByPhase(phase: CfirResolvePhase, target: LLCfirResolveTarget) {
        val lazyResolver = LLCfirLazyPhaseResolverByPhase.getByPhase(phase)
        LLCfirGlobalResolveComponents.getInstance(target.session).lockProvider.withGlobalLock {
            lazyResolver.resolve(target)
        }
    }
}