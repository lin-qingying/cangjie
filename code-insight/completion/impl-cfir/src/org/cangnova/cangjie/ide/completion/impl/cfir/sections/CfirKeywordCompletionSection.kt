package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.ImportCost
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind

/**
 * 关键字、声明名与类型建议候选。
 *
 * 这类候选**不来自作用域**：关键字是语言语法事实，声明名是包内已有符号的拼写，
 * 从作用域取会让关键字永远缺席或让声明名脱离可见性。
 *
 * 关键约束：`this`/`super`/`override` 只有在位置上下文确实存在隐式接收者、
 * 父类型或可覆写成员时才提供——无条件给出会补出在该位置不合法的名字。
 */
@OptIn(CaExperimentalApi::class)
class CfirKeywordCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的关键字与声明名候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = CompletionPositionContexts.classify(position).kind
        if (context == CompletionPositionKind.IMPORT || context == CompletionPositionKind.DOCUMENTATION) {
            return emptyList()
        }

        val file = position.containingCjFile ?: return emptyList()
        val prefix = request.prefix

        return analyze(file) {
            val scopeContext = file.scopeContext(position)
            val snapshots = mutableListOf<CompletionSnapshot>()
            for (keyword in keywordsFor(context, scopeContext.implicitReceivers.isNotEmpty())) {
                if (prefix.isNotEmpty() && !keyword.startsWith(prefix)) continue
                snapshots += keywordSnapshot(keyword)
            }
            // 类型名建议只在类型位置给出。
            //
            // 声明名位置刻意不给：在声明标识符上取导入作用域会回到当前文件的作用域，
            // 与正在解析的声明互相递归，实测触发 StackOverflowError。
            // 声明名建议由顶层区段在成员/局部来源中给出，不与关键字区段混在一起。
            if (context == CompletionPositionKind.TYPE) {
                val importing = file.importingScopeContext.compositeScope()
                for (classifier in importing.classifiers { true }) {
                    val snapshot = classifierSnapshot(
                        classifier,
                        CompletionItemKind.TOP_LEVEL,
                        CompletionOrigin.TOP_LEVEL,
                        ImportCost.ADD_IMPORT,
                        documentStamp = request.documentStamp,
                    )
                    if (prefix.isEmpty() || snapshot.lookupString.startsWith(prefix)) snapshots += snapshot
                }
            }
            snapshots
        }
    }

    /**
     * 按位置形态给出适用关键字。
     *
     * 只列仓颉存在的关键字：不搬 Kotlin 的 `suspend`/`companion`/`by` 等，
     * 也不把 `where`/`defer`/`macro` 等在当前形态下不合法的词塞进来。
     *
     * [hasReceiver] 直接决定 `this` 是否出现：无接收者时给出 `this`
     * 会补出在该位置无法解析的名字。
     */
    private fun keywordsFor(kind: CompletionPositionKind, hasReceiver: Boolean): List<String> {
        val thisKeyword = if (hasReceiver) listOf("this") else emptyList()
        return when (kind) {
            CompletionPositionKind.MEMBER_ACCESS -> thisKeyword
            CompletionPositionKind.FUNCTION_BODY -> thisKeyword + listOf("return", "let", "var", "if", "while", "for")
            CompletionPositionKind.EXPRESSION -> listOf("let", "if", "return") + thisKeyword
            CompletionPositionKind.TYPE -> listOf(
                "class", "struct", "interface", "enum", "extend", "type", "func", "prop", "let", "var",
            )
            CompletionPositionKind.DECLARATION_NAME -> listOf(
                "class", "struct", "interface", "enum", "extend", "func", "prop", "main", "operator",
            )
            CompletionPositionKind.IMPORT -> emptyList()
            CompletionPositionKind.PACKAGE -> listOf("import")
            CompletionPositionKind.NAMED_ARGUMENT -> emptyList()
            CompletionPositionKind.OPERATOR -> listOf("operator")
            CompletionPositionKind.DOCUMENTATION -> emptyList()
            CompletionPositionKind.MACRO_TOKENS -> emptyList()
        }
    }

    /**
     * 构造关键字候选。
     *
     * 关键字身份句柄就是关键字本身：恢复时无需重新解析符号，
     * 但仍要区分同名不同类别的候选。
     */
    private fun keywordSnapshot(keyword: String): CompletionSnapshot = completionSnapshot(
        lookupString = keyword,
        kind = CompletionItemKind.KEYWORD,
        origin = CompletionOrigin.KEYWORD,
        identityKind = CompletionIdentityKind.KEYWORD,
    ).apply {
        this.identity = CompletionIdentityData(
            kind = CompletionIdentityKind.KEYWORD,
            handle = keyword,
            name = keyword,
            deduplicationKey = "keyword:$keyword",
        )
        this.sort = CompletionSortDataImpl(origin = CompletionOrigin.KEYWORD)
    }.build()

    companion object {
        /** 区段标识。 */
        const val ID: String = "cfir.keyword"

        /** 执行优先级：关键字无需作用域解析，最先可用。 */
        const val PRIORITY: Int = 5
    }
}