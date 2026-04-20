/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.psi.CjElement

/**
 * Resolves a single callable declaration.
 * If available, performs partial body analysis.
 */
internal class LLCfirPartialBodyResolveTarget(
    designation: CfirDesignation,
    val request: LLPartialBodyResolveRequest
) : LLCfirResolveTarget(designation) {
    override fun visitTargetElement(element: CfirElementWithResolveState, visitor: LLCfirResolveTargetVisitor) {
        visitor.performAction(element)
    }
}

/**
 * A partial body analysis request.
 *
 * @param target A callable to be analyzed. The [target] is required to be partial body resolvable (see [isPartialBodyResolvable]).
 * @param totalPsiStatementCount The total number of statements in the AST.
 * @param targetPsiStatementCount The number of statements in the AST to be analyzed as a result of this request.
 * @param stopElement The first element that does not belong to the analyzed part of the declaration. If `null`, the whole [target] body
 *        is analyzed.
 */
internal class LLPartialBodyResolveRequest(
    val target: CfirDeclaration,
    val totalPsiStatementCount: Int,
    val targetPsiStatementCount: Int,
    val stopElement: CjElement?
) {
    init {
        require(target.isPartialBodyResolvable)
    }
}