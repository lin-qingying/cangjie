/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry

private object PartialBodyAnalysisStateKey : CfirDeclarationDataKey()

/**
 * Represents the partial (incomplete) body resolve state.
 *
 * If the function body was ever analyzed partially, this attribute must be present.
 * This includes cases when the function was once analyzed partially, and then fully – the attribute still must be there.
 *
 * The attribute must be removed, though, if the declaration phase is reverted (for example, because of in-block modifications).
 *
 * @see LLPartialBodyAnalysisState
 */
internal var CfirDeclaration.partialBodyAnalysisState: LLPartialBodyAnalysisState?
        by CfirDeclarationDataRegistry.data(PartialBodyAnalysisStateKey)

/**
 * [LLCfirResolveTarget] representing single target to resolve. The [target] can be any of [CfirElementWithResolveState]
 */
internal class LLCfirSingleResolveTarget(designation: CfirDesignation) : LLCfirResolveTarget(designation) {
    override fun visitTargetElement(
        element: CfirElementWithResolveState,
        visitor: LLCfirResolveTargetVisitor,
    ) {
        if (element !is CfirFile) {
            visitor.performAction(element)
        }
    }
}
