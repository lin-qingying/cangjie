package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind

/**
 * 局部声明、参数与词法作用域候选。
 *
 * 候选来自 Analysis 的位置作用域上下文，而不是 PSI 祖先遍历：
 * 只有作用域上下文才知道遮蔽次序与可见时机，按名字去重或按 PSI 猜测都会漏候选或给出已被遮蔽的名字。
 *
 * 遮蔽处理按作用域**由内向外**保留首个同名候选：内层先出现就代表外层同名声明已被遮蔽，
 * 把两者都留下会让用户选到当前根本不可用的名字。
 */
@OptIn(CaExperimentalApi::class)
class CfirLocalScopeCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的词法候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = CompletionPositionContexts.classify(position)
        // 成员访问与 import 位置不使用词法候选：它们的候选来自接收者与导入上下文。
        if (context.kind == CompletionPositionKind.MEMBER_ACCESS ||
            context.kind == CompletionPositionKind.IMPORT
        ) {
            return emptyList()
        }

        val prefix = request.prefix
        val file = position.containingCjFile ?: return emptyList()
        // 签名渲染必须在分析内完成：快照离开会话后不得再触碰签名或符号。
        return analyze(file) {
            val scopeContext = file.scopeContext(position)
            // `scopes` 已按真实查找次序从内到外排列，直接顺序遍历即可：
            // 再按 kind.indexInTower 重排会把同一 kind 的层内次序打乱。
            val lexicalScopes = scopeContext.scopes.filter { it.kind == CaScopeKind.LOCAL_SCOPE }
            if (lexicalScopes.isEmpty()) return@analyze emptyList()

            val claimedNames = mutableSetOf<String>()
            buildList {
                for (entry in lexicalScopes) {
                    // 用名称过滤器枚举全部声明：按空名称集合查询只会得到空结果。
                    for (symbol in entry.scope.callables { true }) {
                        if (!matchesPrefix(symbol, prefix)) continue
                        if (!claimedNames.add(symbol.displayName())) continue
                        add(
                            callableSnapshot(
                                symbol,
                                CompletionItemKind.LOCAL,
                                CompletionOrigin.LOCAL_SCOPE,
                                documentStamp = request.documentStamp,
                            ),
                        )
                    }
                }
            }
        }
    }

    /**
     * 判断符号名是否匹配补全前缀。
     */
    private fun matchesPrefix(symbol: CaCallableSymbol, prefix: String): Boolean {
        if (prefix.isEmpty()) return true
        return (symbol as? CaNamedSymbol)?.name?.asString()?.startsWith(prefix) == true
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.localScope"

        /**
         * 执行优先级：词法候选最快也最相关，先于成员与索引候选。
         */
        const val PRIORITY: Int = 10
    }
}