package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjPsiSourceFile
import org.cangnova.cangjie.CjSourceFile

/**
 * 与当前 CFIR 源码偏移同源的文本。
 *
 * PSI 源文件必须取 PSI 自身的文本而不是 VFS 字节：IDE 中文档已提交到 PSI 但尚未落盘时，
 * `virtualFile.inputStream` 仍是旧内容，而 CFIR 的 `startOffset` 来自新的 PSI，两者不同源，
 * 按偏移索引会越界。对齐 Kotlin：`KtSourceElement` 始终锚定 PSI，FIR checker 不直接读文件文本。
 */
internal fun CheckerContext.containingFileText(): CharSequence? {
    val sourceFile: CjSourceFile = containingFileSymbol?.sourceFile ?: return null
    return when (sourceFile) {
        is CjInMemoryTextSourceFile -> sourceFile.text
        is CjPsiSourceFile -> sourceFile.psiFile.text
        else -> sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
    }
}

/**
 * `[offset]` 之前最近一个可索引位置的下标；偏移越过文本长度时收敛到文本末尾之前，越界返回 null。
 *
 * 来源偏移与文本不一致时（如跨文件来源、反序列化来源），偏移可能超出文本长度；
 * 所有按偏移回扫源码的 checker 都必须经由该入口取位置，不能直接对 `text` 下标访问。
 */
internal fun CharSequence.lastIndexBefore(offset: Int): Int? = (minOf(offset, length) - 1).takeIf { it >= 0 }

/** [lastIndexBefore] 位置之前最近的非空白字符下标；此前全是空白或越界时返回 null。 */
internal fun CharSequence.lastNonWhitespaceIndexBefore(offset: Int): Int? {
    var index = lastIndexBefore(offset) ?: return null
    while (index >= 0 && this[index].isWhitespace()) {
        index--
    }
    return index.takeIf { it >= 0 }
}
