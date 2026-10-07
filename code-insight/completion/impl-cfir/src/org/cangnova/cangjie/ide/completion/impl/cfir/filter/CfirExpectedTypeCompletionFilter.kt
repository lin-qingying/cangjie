package org.cangnova.cangjie.ide.completion.impl.cfir.filter

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.ide.completion.api.CompletionExpectedType
import org.cangnova.cangjie.ide.completion.api.CompletionExpectedTypeKind
import org.cangnova.cangjie.ide.completion.api.CompletionExpectedTypeProvider
import org.cangnova.cangjie.ide.completion.api.CompletionFilter
import org.cangnova.cangjie.ide.completion.api.CompletionKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.ExpectedTypeMatch
import org.cangnova.cangjie.ide.completion.api.lookup.toWeight
import org.cangnova.cangjie.ide.completion.api.lookup.withExpectedTypeMatch
import org.cangnova.cangjie.ide.completion.impl.shared.CompletionSnapshotData
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind

/**
 * 基于 Analysis 期望类型的候选过滤器。
 *
 * 三条行为契约（与 [CompletionFilter] 的接口约定一致）：
 * 1. BASIC 不做期望类型过滤——基础补全的语义是「给出全部适用候选并排序」；
 * 2. SMART 在期望类型**未知**时返回与 BASIC 相同的集合：
 *    「算不出期望类型」绝不能退化成「候选为空」；
 * 3. 过滤**不看** [CompletionRequest.invocationCount]：重复按 Tab 不构成访问权限。
 */
@OptIn(CaExperimentalApi::class)
class CfirExpectedTypeCompletionFilter(
    private val expectedTypeProvider: CompletionExpectedTypeProvider = CfirExpectedTypeProvider(),
) : CompletionFilter {
    override fun apply(
        request: CompletionRequest,
        snapshots: List<CompletionSnapshot>,
    ): List<CompletionSnapshot> {
        if (request.kind == CompletionKind.BASIC) return snapshots
        val expectedType = expectedTypeProvider.expectedTypeAt(request)
        // 期望类型未知：不加过滤、不改标记，集合与 BASIC 完全一致。
        if (!expectedType.isKnown) return snapshots

        val file = request.completionPosition.containingCjFile ?: return snapshots
        return analyze(file) {
            val decisions = snapshots.associateWith { matchOf(it, expectedType) }
            snapshots
                .filter { decisions.getValue(it) != ExpectedTypeMatch.MISMATCH }
                .map { it.withExpectedTypeMatch(decisions.getValue(it)) }
        }
    }

    /**
     * 判定单个候选与期望类型的关系。
     *
     * **判不出来时必须返回 [ExpectedTypeMatch.UNKNOWN]**，不能返回 MISMATCH：
     * 期望类型一旦确定就把「推导不出类型」的候选整片删掉，会让列表在
     * 「有期望类型」的位置反而比「无期望类型」的位置更短。
     */
    private fun matchOf(
        snapshot: CompletionSnapshot,
        expectedType: CompletionExpectedType,
    ): ExpectedTypeMatch = when (expectedType.kind) {
        CompletionExpectedTypeKind.NONE -> ExpectedTypeMatch.UNKNOWN
        // 类型位置：只有类型身份的候选合法。变量、函数在类型位置补出来必然报错。
        CompletionExpectedTypeKind.TYPE ->
            if (snapshot.identity.kind == CompletionIdentityKind.TYPE) {
                ExpectedTypeMatch.MATCH
            } else {
                ExpectedTypeMatch.MISMATCH
            }
        CompletionExpectedTypeKind.EXPRESSION ->
            if (expectedType.typeText.isEmpty() || snapshot.typeText.isEmpty()) {
                ExpectedTypeMatch.UNKNOWN
            } else if (expectedType.typeText == snapshot.typeText) {
                ExpectedTypeMatch.MATCH
            } else {
                ExpectedTypeMatch.UNKNOWN
            }
    }
}

/**
 * 基于 Analysis 的期望类型查询。
 *
 * 走 Analysis 的表达式类型组件（类型位置、赋值右值、条件、返回值等），
 * **不在插件侧重写期望类型推断**。
 *
 * 无期望类型的位置返回 [CompletionExpectedType.None]：这是**显式建模的结论**，
 * 与「查了但没查出来」区分开，让调用方能直接跳过 SMART 过滤。
 */
@OptIn(CaExperimentalApi::class)
class CfirExpectedTypeProvider : CompletionExpectedTypeProvider {
    override fun expectedTypeAt(request: CompletionRequest): CompletionExpectedType {
        val position = request.completionPosition
        val file = position.containingCjFile ?: return CompletionExpectedType.None
        // 位置分类走请求级入口：文档注释与宏 token 不在仓颉 PSI 里，
        // 按分析位置分类只会得到 EXPRESSION，期望类型会走错分支。
        val context = request.positionContext
        return analyze(file) {
            when (context.kind) {
                // 类型位置：候选必须是一个类型，不存在「表达式期望类型」。
                CompletionPositionKind.TYPE ->
                    CompletionExpectedType(CompletionExpectedTypeKind.TYPE)
                // 这些位置没有「期望类型」概念：给一个期望类型只会让过滤变得毫无意义。
                CompletionPositionKind.IMPORT,
                CompletionPositionKind.PACKAGE,
                CompletionPositionKind.DOCUMENTATION,
                CompletionPositionKind.MACRO_TOKENS,
                CompletionPositionKind.NAMED_ARGUMENT,
                CompletionPositionKind.OPERATOR,
                CompletionPositionKind.DECLARATION_NAME,
                -> CompletionExpectedType.None
                else -> {
                    val expected = position.expectedType ?: return@analyze CompletionExpectedType.None
                    CompletionExpectedType(
                        kind = CompletionExpectedTypeKind.EXPRESSION,
                        typeText = expected.toString(),
                    )
                }
            }
        }
    }
}

/**
 * 复制快照并替换期望类型匹配结论。
 *
 * 只支持本实现产出的 [CompletionSnapshotData]：匹配结论必须在**分析内**写入，
 * 而快照契约本身不允许界面期回改语义字段。返回类型仍是不可变快照，
 * 替换后的权重按新结论重新折算，保证排序与标记一致。
 */
private fun CompletionSnapshot.withExpectedTypeMatch(match: ExpectedTypeMatch): CompletionSnapshot {
    if (sort.expectedTypeMatch == match) return this
    val data = this as? CompletionSnapshotData
        ?: error("期望类型标记只支持在分析内写入分析期快照，实际 ${this::class.qualifiedName}")
    val newSort = data.sort.withExpectedTypeMatch(match)
    return data.copy(sort = newSort, weight = newSort.toWeight())
}