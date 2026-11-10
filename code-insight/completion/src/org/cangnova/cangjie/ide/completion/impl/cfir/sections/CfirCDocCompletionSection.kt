package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.lexer.cdoc.parser.CDocKnownTag
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement

/**
 * CDoc 注释内的候选：标签名、`@param` 的参数名、引用类标签的声明引用。
 *
 * 仓颉的 CDoc 写作 `/** … */`，标签形如 `@param [value]`、`@see [Document]`——
 * 主语放在方括号里，是一个链接而不是裸名字。三种补全因此形态各异：
 *
 * | 位置 | 候选 | 插入形态 |
 * |---|---|---|
 * | `@` 之后 | 已知标签名 | `param`（**不带 `@`**，光标前的 `@` 已经在了） |
 * | `@param [` 之内 | 所属声明的参数名 | 参数名 |
 * | `@see [` 之内 | 作用域内可见的声明 | 声明名 |
 *
 * **不照搬 KDoc 规则**：标签集合取自 `CDocKnownTag`（仓颉自己的词法解析器定义的枚举），
 * 不从 KDoc 的标签表抄一份；也不补 KDoc 的 `@property`/`@sample` 那套 Kotlin 专属写法。
 *
 * 本区段**不查 Analysis**（除引用候选），因为标签名与参数名都来自 PSI：
 * 在一个注释里跑分析既慢又没有必要。
 */
@OptIn(CaExperimentalApi::class)
class CfirCDocCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算 CDoc 内的候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        // 位置分类用**原始元素**：文档注释不是 CjElement，按分析位置分类只会得到 EXPRESSION。
        val caretElement = request.positionElement
        val context = CompletionPositionContexts.classify(caretElement)
        if (context.kind != CompletionPositionKind.DOCUMENTATION) return emptyList()
        val comment = PsiTreeUtil.getParentOfType(caretElement, PsiComment::class.java) ?: return emptyList()
        val typed = textBeforeCaretInLine(comment, caretElement) ?: return emptyList()
        val prefix = request.prefix

        tagNamePrefix(typed)?.let { typedPrefix -> return tagNameCandidates(typedPrefix, comment) }
        subjectTagName(typed)?.let { tag ->
            return when (tag) {
                CDocKnownTag.PARAM -> parameterCandidates(documentedDeclaration(comment, context.declaration), prefix)
                else -> if (tag.isReferenceRequired) referenceCandidates(request, prefix) else emptyList()
            }
        }
        return emptyList()
    }

    /**
     * 找出这条文档注释所描述的声明。
     *
     * 不能只看位置所在声明：文档注释通常写在声明**之前**，此时它是文件的直接子节点，
     * `classify` 给出的 `declaration` 为 `null`——而 `@param` 要的正是「这条注释在描述谁」。
     * 两种写法都要认：注释写在声明内部（在某些解析形态下）与写在声明之前。
     */
    private fun documentedDeclaration(comment: PsiComment, fallback: CjElement?): CjElement? {
        PsiTreeUtil.getParentOfType(comment, CjDeclaration::class.java)?.let { return it }
        // 更稳的判据：**第一条起点在这条注释之后的 callable 声明**。
        // 不去逐级爬 nextSibling——注释与声明之间可能夹着任意空白、换行与兄弟节点，
        // 爬链在真实文件里很容易在遇到别的成员时提前放弃。
        val file = comment.containingFile ?: return fallback
        return PsiTreeUtil.findChildrenOfType(file, CjCallableDeclaration::class.java)
            .firstOrNull { it.textRange.startOffset >= comment.textRange.endOffset }
            ?: fallback
    }

    /**
     * 标签名候选。
     *
     * 已经出现过的标签不再给：同一个标签写两遍在 CDoc 里没有意义，
     * 而列出来只会让用户重复选中。
     */
    private fun tagNameCandidates(prefix: String, comment: PsiComment): List<CompletionSnapshot> {
        val used = usedTagNames(comment.text)
        return CDocKnownTag.entries
            .map { it.name.lowercase() }
            .filter { it !in used }
            .filter { it.startsWith(prefix) }
            .sorted()
            .map { name ->
                completionSnapshotFor(
                    lookupString = name,
                    identityKind = CompletionIdentityKind.DOCUMENTATION,
                )
            }
    }

    /**
     * `@param` 的参数名候选。
     *
     * 参数名取自**所属声明的形参列表**，不是全文件的名字：`@param` 的语义就是
     * 「这个函数的这个参数」，给用户一堆无关名字等于把语义丢掉。
     */
    private fun parameterCandidates(declaration: CjElement?, prefix: String): List<CompletionSnapshot> {
        val callable = PsiTreeUtil.getParentOfType(declaration, CjCallableDeclaration::class.java)
            ?: (declaration as? CjCallableDeclaration)
            ?: return emptyList()
        return callable.valueParameters
            .mapNotNull { it.name }
            .filter { it.startsWith(prefix) }
            .map { name ->
                completionSnapshotFor(
                    lookupString = name,
                    identityKind = CompletionIdentityKind.DOCUMENTATION,
                )
            }
    }

    /**
     * 引用类标签（`@see` / `@throws` 等）的声明引用候选。
     *
     * 候选来自文件的导入作用域——与代码位置看到的是同一批名字，
     * 但这里它们是**文档引用**：类别记为 `DOCUMENTATION`，插入形态与代码里的引用一致。
     */
    private fun referenceCandidates(request: CompletionRequest, prefix: String): List<CompletionSnapshot> {
        val file = request.completionPosition.containingCjFile ?: return emptyList()
        return analyze(file) {
            buildList {
                for (entry in file.importingScopeContext.scopes) {
                    if (entry.kind == CaScopeKind.LOCAL_SCOPE) continue
                    for (classifier in entry.scope.classifiers { true }) {
                        val name = (classifier as? CaNamedSymbol)?.name?.asString() ?: continue
                        if (!name.startsWith(prefix)) continue
                        add(
                            classifierSnapshot(
                                classifier,
                                CompletionItemKind.DOCUMENTATION,
                                CompletionOrigin.DOCUMENTATION,
                                documentStamp = request.documentStamp,
                            ),
                        )
                    }
                }
            }
        }
    }

    /**
     * 取出注释内**光标所在行、行首到光标**的文本，并剥掉注释前导。
     *
     * 用文本而不是 CDoc PSI 判定子上下文：补全位置上 PSI 往往还没解析成完整的标签结构
     * （用户正打到一半），按结构找会漏掉最常见的时刻。
     */
    private fun textBeforeCaretInLine(comment: PsiComment, position: PsiElement): String? {
        val offsetInComment = position.textOffset - comment.textOffset
        if (offsetInComment < 0 || offsetInComment > comment.textLength) return null
        val upToCaret = comment.text.substring(0, offsetInComment)
        val line = upToCaret.substringAfterLast('\n')
        return line.trimStart().removePrefix("*").trimStart()
    }

    /**
     * 若光标处在标签名位置，返回已键入的标签名前缀（不含 `@`）。
     */
    private fun tagNamePrefix(typed: String): String? {
        val match = TAG_NAME_PATTERN.matchEntire(typed) ?: return null
        return match.groupValues[1]
    }

    /**
     * 若光标处在某个标签的主语位置，返回该标签。
     */
    private fun subjectTagName(typed: String): CDocKnownTag? {
        val match = SUBJECT_PATTERN.matchEntire(typed) ?: return null
        return CDocKnownTag.findByTagName(match.groupValues[1])
    }

    /**
     * 收集注释里已经出现过的标签名（小写）。
     */
    private fun usedTagNames(commentText: String): Set<String> =
        TAG_OCCURRENCE_PATTERN.findAll(commentText).map { it.groupValues[1].lowercase() }.toSet()

    /**
     * 构造一个 CDoc 候选。
     *
     * 身份类别固定为 `DOCUMENTATION`：文档位置上的候选不是可插入的代码符号，
     * 其“是否仍然有效”无法用符号恢复回答（参数名不是可恢复的顶层声明），
     * 而这个类别在复查里被明确定义为**永不失效**。
     */
    private fun completionSnapshotFor(
        lookupString: String,
        identityKind: CompletionIdentityKind,
    ): CompletionSnapshot = org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot(
        lookupString = lookupString,
        kind = CompletionItemKind.DOCUMENTATION,
        origin = CompletionOrigin.DOCUMENTATION,
        identityKind = identityKind,
    ).build()

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.cdoc"

        /**
         * 执行优先级：标签与参数候选只读 PSI，比任何需要分析的区段都快。
         */
        const val PRIORITY: Int = 15

        /** 行首为 `@` 加若干标签名字符。 */
        private val TAG_NAME_PATTERN = Regex("""@(\w*)""")

        /** 行首为 `@标签名` 加空白再加未闭合的方括号主语。 */
        private val SUBJECT_PATTERN = Regex("""@(\w+)\s+\[\w*""")

        /** 注释中已出现的标签。 */
        private val TAG_OCCURRENCE_PATTERN = Regex("""@(\w+)""")
    }
}
