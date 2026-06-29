/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext

/**
 * Represents the partial (incomplete) body resolve state.
 * The attribute is only present if the function body was analyzed partially.
 *
 * If the attribute is added, it must never be removed. For a fully analyzed declaration, [analyzedPsiStatementCount] is equal to
 * [totalPsiStatementCount]. Also, [analysisStateSnapshot] is kept `null` as unneeded (a fully analyzed function is never analyzed again).
 */
internal data class LLPartialBodyAnalysisState(
    /**
     * A number of topmost [org.cangnova.cangjie.psi.CjExpression] statements in the declaration's body block.
     *
     * A topmost statement is a statement directly nested inside the declaration's body block.
     * Such as, in the following example, `foo()` and `baz {}` are topmost statements while `baz()` is not.
     *
     * ```
     * fun test() {
     *     foo()
     *     bar {
     *         baz()
     *     }
     * }
     * ```
     */
    val totalPsiStatementCount: Int,

    /**
     * A number of analyzed [org.cangnova.cangjie.psi.CjExpression] statements in the declaration's body block.
     * @see totalPsiStatementCount
     */
    val analyzedPsiStatementCount: Int,

    /**
     * A number of analyzed [CfirStatement] in the declaration's body block.
     */
    val analyzedCfirStatementCount: Int,

    /**
     * A number of partial body analyses happened with the declaration up to this point.
     * Note that even if [performedAnalysesCount] is greater than zero, only function signature parts (e.g., default parameter values)
     * might be analyzed.
     *
     * After a certain number of analyses, the resolution engine might decide to perform full body analysis.
     */
    val performedAnalysesCount: Int,

    /**
     * Data required for subsequent analysis.
     * [analysisStateSnapshot] is `null` if the declaration is fully analyzed.
     */
    val analysisStateSnapshot: LLPartialBodyAnalysisSnapshot?
) {
    /**
     * Whether a declaration is fully analyzed.
     * Prefer [org.cangnova.cangjie.cfir.declarations.CfirResolveState.resolvePhase] if you have a declaration in hands.
     */
    val isFullyAnalyzed: Boolean
        get() = totalPsiStatementCount == analyzedPsiStatementCount

    override fun toString(): String {
        return "$analyzedCfirStatementCount($analyzedPsiStatementCount/$totalPsiStatementCount) #$performedAnalysesCount"
    }
}

/**
 * partial body analysis 后续增量解析需要恢复的控制流与作用域上下文快照。
 */
internal class LLPartialBodyAnalysisSnapshot(
    /**
     * 已解析语句和默认参数值的结果集合。
     */
    val result: LLPartialBodyAnalysisResult,
    /**
     * partial analysis 停止位置对应的 tower data 上下文。
     */
    val towerDataContext: CfirTowerDataContext,
    /**
     * partial analysis 停止位置对应的数据流分析上下文。
     */
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext
)

/**
 * Contains already resolved parts of the declaration.
 *
 * In cases when a declaration may be only partially resolved, access the resolved state through this class instead of checking
 * the CFIR tree directly (such as getting statements from [org.cangnova.cangjie.cfir.declarations.CfirFunction.body]),
 * as these parts can be concurrently replaced with lazy blocks/expressions by a state keeper.
 */
internal class LLPartialBodyAnalysisResult(
    /**
     * A list of already resolved statements.
     * The list contains [LLPartialBodyAnalysisState.analyzedCfirStatementCount] first statements from the function body.
     */
    val statements: List<CfirStatement>,
    /**
     * A list of resolved default parameter values.
     * The list is empty if there are no value parameters with default values.
     *
     * Now, this does not include corresponding parameter names as this has never been needed.
     */
    val defaultParameterValues: List<CfirExpression>
)
