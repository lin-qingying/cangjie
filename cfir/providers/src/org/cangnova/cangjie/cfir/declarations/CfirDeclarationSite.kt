package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.source.CjRealPsiSourceElement

/**
 * 声明的**源码位置身份**。
 *
 * 同一处源码声明在低阶 CFIR 里可能有多份表示：实时 raw 树、宏语义快照的展开树（成员可能已被
 * 宏改写），以及历史上已失效的树。它们是同一处声明的不同视图，不是重声明。类型解析、重声明
 * 判定与候选聚合必须以源码位置为身份，并让**当前已发布视图**优先，而不是按 CFIR 符号对象或
 * 成员内容比较。
 *
 * - 真实 PSI 声明：`文件路径:起始偏移:结束偏移`；
 * - 生成/合成声明：没有稳定源码位置，用来源对象的身份区分，避免误合并不同生成声明。
 */
fun CfirClassLikeSymbol<*>.declarationSiteId(): String? {
    val source = cfir.source ?: return null
    if (source !is CjRealPsiSourceElement) {
        return "source:${System.identityHashCode(source)}"
    }
    val psi = source.psi
    val path = runCatching { psi.containingFile?.virtualFile?.path }.getOrNull() ?: return null
    val range = runCatching { psi.textRange }.getOrNull() ?: return null
    return "$path:${range.startOffset}:${range.endOffset}"
}
