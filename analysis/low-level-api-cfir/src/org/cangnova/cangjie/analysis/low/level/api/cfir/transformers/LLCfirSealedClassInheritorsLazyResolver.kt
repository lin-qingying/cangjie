/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

internal object LLCfirSealedClassInheritorsLazyResolver : LLCfirLazyResolver(CfirResolvePhase.SEALED_CLASS_INHERITORS) {
    override fun createTargetResolver(
        target: LLCfirResolveTarget,
    ): LLCfirTargetResolver = LLCfirSealedClassInheritorsDesignatedResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {}
}


/**
 * This resolver is responsible for [SEALED_CLASS_INHERITORS][CfirResolvePhase.SEALED_CLASS_INHERITORS] phase.
 *
 * LL CFIR uses [LLSealedInheritorsProvider][org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLSealedInheritorsProvider]
 * instead of the compiler [SealedClassInheritorsProviderImpl][org.cangnova.cangjie.cfir.declarations.SealedClassInheritorsProviderImpl],
 * so it does nothing during this phase as sealed inheritors will be provided later on demand.
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLSealedInheritorsProvider
 * @see org.cangnova.cangjie.cfir.declarations.SealedClassInheritorsProvider
 * @see CfirResolvePhase.SEALED_CLASS_INHERITORS
 */
private class LLCfirSealedClassInheritorsDesignatedResolver(target: LLCfirResolveTarget) : LLCfirTargetResolver(
    target,
    CfirResolvePhase.SEALED_CLASS_INHERITORS,
) {
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        // just update the phase
    }
}
