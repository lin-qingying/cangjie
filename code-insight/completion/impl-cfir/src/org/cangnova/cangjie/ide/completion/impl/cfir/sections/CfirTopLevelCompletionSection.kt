package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind

/**
 * 顶层声明、类型与包候选。
 *
 * 候选来自 Analysis 的**导入作用域上下文**：它已经按编译器顺序组合了本文件顶层声明、
 * 同包成员、显式导入（含别名）与语言默认导入。按 PSI 声明列表自行拼候选会丢掉
 * 导入别名与来源优先级，也无法区分「已导入」与「需要导入」。
 *
 * 导入来源决定排序成本：已在本文件作用域可见的候选排在需要追加 import 的候选之前，
 * 这一区分由 `importCost` 表达，而不是由两个区段各自硬编码权重。
 *
 * 成员访问位置不使用本区段：那里的候选必须来自接收者类型。
 */
@OptIn(CaExperimentalApi::class)
class CfirTopLevelCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的顶层与导入候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = request.positionContext.kind
        // 成员访问只补接收者成员；import 位置由导入区段负责。
        if (context == CompletionPositionKind.MEMBER_ACCESS) return emptyList()

        val file = position.containingCjFile ?: return emptyList()
        val prefix = request.prefix
        return analyze(file) {
            val importing = file.importingScopeContext
            buildList {
                for (entry in importing.scopes) {
                    // 词法层由局部区段负责；这里只取文件、包与导入来源。
                    if (entry.kind == CaScopeKind.LOCAL_SCOPE) continue
                    // 文件与包作用域的声明在当前文件已直接可达；导入层同样无需再写 import。
                    val origin = originFor(entry.kind)
                    for (classifier in entry.scope.classifiers { true }) {
                        val snapshot = classifierSnapshot(
                            classifier,
                            CompletionItemKind.TOP_LEVEL,
                            origin,
                            ImportCost.DIRECT,
                            documentStamp = request.documentStamp,
                        )
                        if (prefix.isEmpty() || snapshot.lookupString.startsWith(prefix)) add(snapshot)
                    }
                    for (callable in entry.scope.callables { true }) {
                        if (!callable.matchesPrefix(prefix)) continue
                        add(
                            callableSnapshot(
                                callable,
                                CompletionItemKind.TOP_LEVEL,
                                origin,
                                ImportCost.DIRECT,
                                documentStamp = request.documentStamp,
                            ),
                        )
                    }
                }
            }.distinctByIdentity()
        }
    }

    /**
     * 把作用域来源映射为候选来源。
     *
     * 包成员与导入来源分开：前者是「同包直接可达」，后者是「靠 import 可达」，
     * 二者在需要追加 import 与冲突时的处理并不相同。
     */
    private fun originFor(kind: CaScopeKind): CompletionOrigin = when {
        kind.isImporting -> CompletionOrigin.IMPORT
        kind == CaScopeKind.PACKAGE_MEMBER_SCOPE -> CompletionOrigin.TOP_LEVEL
        else -> CompletionOrigin.TOP_LEVEL
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.topLevel"

        /**
         * 执行优先级：顶层候选需要作用域拼接，慢于词法与成员候选。
         */
        const val PRIORITY: Int = 30
    }
}

/**
 * 判断 callable 名称是否匹配补全前缀。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaCallableSymbol.matchesPrefix(prefix: String): Boolean =
    prefix.isEmpty() || (this as? CaNamedSymbol)?.name?.asString()?.startsWith(prefix) == true

/**
 * 判断分类器名称是否匹配补全前缀。
 */
@OptIn(CaExperimentalApi::class)
internal fun CaClassifierSymbol.matchesClassifierPrefix(prefix: String): Boolean =
    prefix.isEmpty() || (this as? CaNamedSymbol)?.name?.asString()?.startsWith(prefix) == true