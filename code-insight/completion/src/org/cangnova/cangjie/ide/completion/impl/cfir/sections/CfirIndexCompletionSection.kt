@file:OptIn(org.cangnova.cangjie.analysis.api.CaExperimentalApi::class)

package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateStatus
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.statistics.CompletionStage
import org.cangnova.cangjie.ide.completion.api.statistics.completionStatistics
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportPlan
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionImportTargetData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind

/**
 * 索引召回区段：补全未导入 / 跨模块的声明。
 *
 * 作用域路径看不到这些名字——导入作用域里根本没有它们，用户不手写包路径就补不出来。
 * 名称发现由 Analysis 完成，分两路：源码声明的名字来自**宿主名称索引**（按前缀），
 * 库侧（`.cjo`）声明的名字来自 CFIR 的只读名字视图。本区段只负责把两路接进同一套候选管线。
 *
 * 三条边界：
 * 1. **不自己解析名称**：名称发现给名字，符号恢复在 Analysis 层，候选与作用域路径同源；
 * 2. **可见性与可达性仍由 Analysis 判定**：索引范围不等于访问权限，
 *    private/protected 与遮蔽必须在使用点过滤；
 * 3. **索引不可用时不退化**：源码模块的候选必须缺席——它们只能靠索引找到，
 *    一旦出现就说明有人遍历了源码文件；禁止全工作区声明扫描兜底。
 *    库侧候选不依赖宿主索引，仍须照常给出：降级是「少给」，不是「不给」。
 *
 * 召回范围是「当前文件之外」的名字：本文件自己的顶层声明本就在文件/包作用域里，
 * 作用域区段必定给出，索引再列一次只会重复。
 */
class CfirIndexCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
    isDelayed = IS_DELAYED,
) {
    /**
     * 计算索引召回候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val kind = request.positionContext.kind
        if (kind !in SUPPORTED_POSITIONS) return emptyList()
        val file = position.containingCjFile ?: return emptyList()
        // 重复触发时放宽匹配：用户再次请求补全，通常意味着上一轮前缀太窄。
        val repeated = request.invocationCount > 1
        // 索引区段是整条管线里最慢的一步，单独记一笔：它还能回答「索引到底有没有被用到」。
        val statistics = completionStatistics(file.project)
        val startedNanos = System.nanoTime()

        return analyze(file) {
            // 本文件自己的顶层声明符号：索引召回只服务「当前文件之外」的名字。
            // 文件自己的声明已经在文件/包作用域里，作用域区段必定给出它，再召回一次只是重复；
            // 而**被**本文件声明遮蔽的跨包同名候选由统一判定器的 SHADOWED 结论负责。
            // 这里必须用符号等价性而不是名字或 DIRECT 状态来判：已导入的名字也可能是 DIRECT，
            // 复查路径依赖索引区段照常给出它们；只有「就是本文件这一条声明」才是重复项。
            // 延迟到真的出现候选再构造符号：空候选时不必为整份文件的声明建符号。
            var ownDeclarations: List<CaSymbol>? = null
            buildList {
                for (symbol in getDeclarationsByNamePrefix(request.prefix, repeated)) {
                    ProgressManager.checkCanceled()
                    // 类型位置只收类型：把函数名放进类型槽位，用户选中即编译失败。
                    if (kind == CompletionPositionKind.TYPE && symbol !is CaClassifierSymbol) continue
                    val name = symbol.name?.asString() ?: continue
                    val own = ownDeclarations
                        ?: file.declarations
                            // 补全发生在未完成源码上：个别声明恢复不出符号时跳过该条。
                            // 只捕获「符号不可用」这一预期失败类型——其它异常仍是缺陷，
                            // 不能被这条降级路径顺手吞掉（否则真问题永远只表现为「少一个候选」）。
                            .mapNotNull { declaration ->
                                try {
                                    declaration.symbol
                                } catch (unavailable: CaSymbolUnavailableException) {
                                    unavailable.logCompletionDegradation("index declaration")
                                    null
                                }
                            }
                            .also { ownDeclarations = it }
                    if (own.any { declaration -> declaration.isEquivalentTo(symbol) }) continue
                    // 单个符号的判定与渲染整体隔离：索引里只要有一条符号在本会话不可恢复，
                    // 就不能让它把整段索引候选带走（与顶层区段同一策略）。
                    val candidate = candidateOrNull {
                        // 统一判定器负责访问权限、位置遮蔽、默认导入和精确导入路径；
                        // 索引只负责找出名字，不能在这里另造一套可达性与 import 规则。
                        val decision = symbol.checkCompletionCandidate(position)
                        val importResolution = when (decision.status) {
                            CaCompletionCandidateStatus.DIRECT -> ImportResolution.NoImport
                            CaCompletionCandidateStatus.REQUIRES_IMPORT -> ImportResolution(
                                CompletionImportPlan.AddImport(
                                    CompletionImportTargetData(
                                        importPath = checkNotNull(decision.requiredImport) {
                                            "REQUIRES_IMPORT 候选必须提供 requiredImport: ${decision.symbol}"
                                        },
                                        expressionText = name,
                                    ),
                                ),
                            )
                            CaCompletionCandidateStatus.HIDDEN,
                            CaCompletionCandidateStatus.NOT_APPLICABLE,
                            CaCompletionCandidateStatus.UNKNOWN,
                            -> return@candidateOrNull null
                        }
                        val importCost = if (decision.status == CaCompletionCandidateStatus.DIRECT) {
                            ImportCost.DIRECT
                        } else {
                            ImportCost.ADD_IMPORT
                        }
                        when (symbol) {
                            is CaClassifierSymbol -> classifierSnapshot(
                                symbol,
                                CompletionItemKind.TOP_LEVEL,
                                CompletionOrigin.TOP_LEVEL,
                                importCost,
                                request.documentStamp,
                                importResolution,
                            )

                            is CaCallableSymbol -> callableSnapshot(
                                symbol,
                                if (symbol is CaEnumConstructorSymbol) CompletionItemKind.ENUM_ENTRY
                                else CompletionItemKind.TOP_LEVEL,
                                CompletionOrigin.TOP_LEVEL,
                                importCost,
                                request.documentStamp,
                                importResolution,
                            )

                            else -> null
                        }
                    }
                    if (candidate != null) add(candidate)
                }
            }
        }.also {
            statistics.recordStage(CompletionStage.INDEX_QUERY, System.nanoTime() - startedNanos)
        }
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.index"

        /**
         * 执行优先级：索引候选最慢，排在所有作用域候选之后。
         */
        const val PRIORITY: Int = 50

        /**
         * 属于**延迟阶段**：索引要枚举源码倒排表与 `.cjo` 名字视图，是整条管线里最慢的一步。
         * 用户应当先看到作用域候选，索引候选随后补上；继续输入时它应当整段被跳过。
         */
        const val IS_DELAYED: Boolean = true

        /**
         * 使用索引的位置。
         *
         * 成员访问、import、命名实参与文档位置不走索引：前两者的候选来自接收者与导入上下文，
         * 命名实参来自被调用方签名，文档标签来自语言；索引召回在这些位置只会给出无关名字。
         */
        private val SUPPORTED_POSITIONS = setOf(
            CompletionPositionKind.EXPRESSION,
            CompletionPositionKind.FUNCTION_BODY,
            CompletionPositionKind.TYPE,
            CompletionPositionKind.CODE_FRAGMENT_EXPRESSION,
            CompletionPositionKind.CODE_FRAGMENT_BLOCK,
        )
    }
}

/**
 * 渲染单个索引候选；符号不可用时丢弃该候选，其余候选照常产出。
 *
 * 只捕获 [CaSymbolUnavailableException]：判定器或渲染链其它异常仍是缺陷，必须暴露，
 * 不能被这条降级路径顺手吞成「少一个候选」。
 */
private inline fun candidateOrNull(render: () -> CompletionSnapshot?): CompletionSnapshot? = try {
    render()
} catch (unavailable: CaSymbolUnavailableException) {
    unavailable.logCompletionDegradation("index candidate")
    null
}
