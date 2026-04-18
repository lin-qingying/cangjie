/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.utils.isConst
import org.cangnova.cangjie.cfir.expressions.CfirExpressionEvaluator

internal object LLCfirConstantEvaluationLazyResolver : LLCfirLazyResolver(CfirResolvePhase.CONSTANT_EVALUATION) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirConstantEvaluationTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {}
}

/**
 * This resolver is responsible for [CONSTANT_EVALUATION][CfirResolvePhase.CONSTANT_EVALUATION] phase.
 *
 * @see CfirResolvePhase.CONSTANT_EVALUATION
 */
private class LLCfirConstantEvaluationTargetResolver(resolveTarget: LLCfirResolveTarget) : LLCfirTargetResolver(
    resolveTarget,
    CfirResolvePhase.CONSTANT_EVALUATION,
) {
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        if (target is CfirProperty && target.isConst) {
            target.evaluatedInitializer = CfirExpressionEvaluator.evaluatePropertyInitializer(target, target.moduleData.session)
        }
    }
}
