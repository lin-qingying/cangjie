package org.cangnova.cangjie.lsp.completion

import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot

/**
 * LSP 侧候选排序文本。
 *
 * 排序在**分析期**已由上游算完并冻结在快照里（[CompletionSnapshot.sort] 与权重），
 * 服务端返回的列表即最终顺序。LSP 只允许客户端按 `sortText` 做字符串比较，
 * 因此这里把「列表下标」零填充后直接当 `sortText`：
 * 客户端比较的字符串序与服务端的列表序**按构造一致**，不依赖两端各自实现比较函数。
 *
 * 不直接使用权重值：权重是分析期的位段编码，它的数值大小是内部约定；
 * 把它暴露成 `sortText` 等于让客户端参与解释内部编码，一旦位段调整就会与 IDE 端排序漂移。
 */
object LspCompletionSortingUtil {
    /**
     * 把候选在最终列表中的下标转成 `sortText`。
     */
    fun sortTextOf(index: Int): String {
        require(index >= 0) { "候选下标必须非负，实际 $index" }
        return index.toString().padStart(WIDTH, '0')
    }

    /**
     * 零填充宽度。
     *
     * 6 位足够覆盖一次补全的候选规模，且远小于任何客户端对 `sortText` 长度的限制；
     * 宽度固定才能保证「先比较短串」的客户端也得到正确顺序。
     */
    private const val WIDTH: Int = 6
}
