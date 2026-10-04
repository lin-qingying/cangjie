package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.ide.completion.api.CaCompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CaCompletionRequest
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentityData
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionSortDataImpl
import org.cangnova.cangjie.ide.completion.api.lookup.CaImportCost
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.caCompletionSnapshot
import org.cangnova.cangjie.ide.completion.impl.shared.context.CaCompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CaCompletionPositionKind

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
    override fun compute(request: CaCompletionRequest): List<CaCompletionSnapshot> {
        val position = request.completionPosition
        val context = CaCompletionPositionContexts.classify(position).kind
        if (context == CaCompletionPositionKind.IMPORT || context == CaCompletionPositionKind.DOCUMENTATION) {
            return emptyList()
        }

        val file = position.containingCjFile ?: return emptyList()
        val prefix = request.prefix

        return analyze(file) {
            val scopeContext = file.scopeContext(position)
            val snapshots = mutableListOf<CaCompletionSnapshot>()
            for (keyword in keywordsFor(context, scopeContext.implicitReceivers.isNotEmpty())) {
                if (prefix.isNotEmpty() && !keyword.startsWith(prefix)) continue
                snapshots += keywordSnapshot(keyword)
            }
            // 类型名建议只在类型位置给出。
            //
            // 声明名位置刻意不给：在声明标识符上取导入作用域会回到当前文件的作用域，
            // 与正在解析的声明互相递归，实测触发 StackOverflowError。
            // 声明名建议由顶层区段在成员/局部来源中给出，不与关键字区段混在一起。
            if (context == CaCompletionPositionKind.TYPE) {
                val importing = file.importingScopeContext.compositeScope()
                for (classifier in importing.classifiers { true }) {
                    val snapshot = classifierSnapshot(
                        classifier,
                        CaCompletionItemKind.TOP_LEVEL,
                        CaCompletionOrigin.TOP_LEVEL,
                        CaImportCost.ADD_IMPORT,
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
    private fun keywordsFor(kind: CaCompletionPositionKind, hasReceiver: Boolean): List<String> {
        val thisKeyword = if (hasReceiver) listOf("this") else emptyList()
        return when (kind) {
            CaCompletionPositionKind.MEMBER_ACCESS -> thisKeyword
            CaCompletionPositionKind.FUNCTION_BODY -> thisKeyword + listOf("return", "let", "var", "if", "while", "for")
            CaCompletionPositionKind.EXPRESSION -> listOf("let", "if", "return") + thisKeyword
            CaCompletionPositionKind.TYPE -> listOf(
                "class", "struct", "interface", "enum", "extend", "type", "func", "prop", "let", "var",
            )
            CaCompletionPositionKind.DECLARATION_NAME -> listOf(
                "class", "struct", "interface", "enum", "extend", "func", "prop", "main", "operator",
            )
            CaCompletionPositionKind.IMPORT -> emptyList()
            CaCompletionPositionKind.PACKAGE -> listOf("import")
            CaCompletionPositionKind.NAMED_ARGUMENT -> emptyList()
            CaCompletionPositionKind.OPERATOR -> listOf("operator")
            CaCompletionPositionKind.DOCUMENTATION -> emptyList()
            CaCompletionPositionKind.MACRO_TOKENS -> emptyList()
        }
    }

    /**
     * 构造关键字候选。
     *
     * 关键字身份句柄就是关键字本身：恢复时无需重新解析符号，
     * 但仍要区分同名不同类别的候选。
     */
    private fun keywordSnapshot(keyword: String): CaCompletionSnapshot = caCompletionSnapshot(
        lookupString = keyword,
        kind = CaCompletionItemKind.KEYWORD,
        origin = CaCompletionOrigin.KEYWORD,
        identityKind = CaCompletionIdentityKind.KEYWORD,
    ).apply {
        this.identity = CaCompletionIdentityData(
            kind = CaCompletionIdentityKind.KEYWORD,
            handle = keyword,
            name = keyword,
            deduplicationKey = "keyword:$keyword",
        )
        this.sort = CaCompletionSortDataImpl(origin = CaCompletionOrigin.KEYWORD)
    }.build()

    companion object {
        /** 区段标识。 */
        const val ID: String = "cfir.keyword"

        /** 执行优先级：关键字无需作用域解析，最先可用。 */
        const val PRIORITY: Int = 5
    }
}