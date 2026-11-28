package org.cangnova.cangjie.ide.completion.impl.shared.insert

import com.intellij.openapi.editor.Document
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.InsertionValidity

/**
 * 插入前校验。
 *
 * 补全快照在**分析期**冻结，插入发生在**之后**，其间文档可能已被键入、格式化
 * 或协同编辑修改。继续用旧括号判断写入，会插入到错误位置或破坏用户编辑。
 *
 * 因此插入的第一件事是校验，不是写入。校验不通过时**拒绝写入**而不是尽力修正：
 * 「猜一个大概正确的位置」比「不插」更危险，因为用户不会注意到差别。
 */
object InsertionValidator {
    /**
     * 校验快照在当前文档状态下是否仍然可以写入。
     *
     * @param snapshot 待插入的候选快照。
     * @param startOffset 替换区间起点（平台给出，是本次插入的真实起点）。
     * @param tailOffset 替换区间终点。
     * @param document 当前文档。
     * @param selectionStamp IntelliJ 替换查找串前捕获的文档修改戳；缺失时拒绝写入。
     */
    fun validate(
        snapshot: CompletionSnapshot,
        startOffset: Int,
        tailOffset: Int,
        document: Document,
        selectionStamp: Long?,
        revalidation: RevalidationVerdict = RevalidationVerdict.NotPerformed,
    ): InsertionValidity {
        // 语义目标已失效时**先于**文档版本判定：候选指向的声明都不在了，
        // 再讨论「文档有没有变」没有意义，而且插入结果一定是个未解析的名字。
        if (revalidation == RevalidationVerdict.Invalid) {
            return InsertionValidity.DanglingCandidate(snapshot.identity)
        }
        val versionValidity = validateDocumentStamp(snapshot, selectionStamp)
        if (!versionValidity.writable) return versionValidity
        return if (isRangeSane(startOffset, tailOffset, document.textLength)) {
            InsertionValidity.Valid
        } else {
            InsertionValidity.OutOfRange(startOffset, tailOffset, document.textLength)
        }
    }

    /**
     * 校验选中候选前捕获的文档版本。
     *
     * 平台会在 `beforeItemSelected` 之后、仓颉插入处理器之前替换查找串；该次平台写入
     * 不代表用户编辑。这里必须使用替换前捕获的戳，不能回退到插入处理器读取的当前戳。
     */
    fun validateDocumentStamp(snapshot: CompletionSnapshot, selectionStamp: Long?): InsertionValidity {
        if (!snapshot.requiresDocumentStamp()) return InsertionValidity.Valid
        val actual = selectionStamp ?: return InsertionValidity.SelectionContextUnavailable
        val expected = snapshot.insertion.documentStamp
        return if (expected == actual) {
            InsertionValidity.Valid
        } else {
            InsertionValidity.StaleDocument(expected, actual)
        }
    }

    /**
     * 判断替换区间是否仍落在文档内。
     *
     * 区间越界几乎总是因为文档在分析后被缩短：继续写入会抛
     * `IndexOutOfBoundsException` 并中断整个插入动作，把一次无害的过期候选
     * 升级成 IDE 侧的可见故障。
     */
    private fun isRangeSane(startOffset: Int, tailOffset: Int, documentLength: Int): Boolean =
        startOffset >= 0 && tailOffset >= startOffset && tailOffset <= documentLength
}

/**
 * 该候选的插入是否依赖文档版本戳。
 *
 * 依赖版本的候选，其括号、实参与 caret 结论都是在**具体文本**上算出来的；
 * 不依赖版本的候选（关键字、包名）只写入自身文本，与周围文本无关。
 */
private fun CompletionSnapshot.requiresDocumentStamp(): Boolean =
    identity.kind != CompletionIdentityKind.KEYWORD
