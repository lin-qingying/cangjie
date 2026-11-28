package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.source.toSourceLinesMapping
import java.util.UUID

/** 冻结词法输入中的实际词元身份；序号属于该输入，不以声明名或源码偏移代替身份。 */
data class CjSourceTokenId(val sourceIdentity: UUID, val ordinal: Int) {
    init {
        require(ordinal >= 0)
    }
}

/** 全文词法输入的持久化描述；只验证引用完整性，不授予 live owner 或跨语境文法等价。 */
data class CjLexicalSourceDescriptor(val sourceIdentity: UUID, val tokenCount: Int) {
    init {
        require(tokenCount >= 0)
    }

    fun contains(token: CjSourceTokenId): Boolean =
        token.sourceIdentity == sourceIdentity && token.ordinal in 0 until tokenCount
}

/** 一份实际解析输入的不可变全文快照；匿名输入明确没有外部文件名或路径。 */
class CjParsingSourceSnapshot(
    val fileName: String?,
    val filePath: String?,
    val text: String,
) {
    /** 文本版本身份与 parser journal 的事务身份独立，惰性子根可以共享同一份快照。 */
    val identity: UUID = UUID.randomUUID()
    private val linesMapping = text.toSourceLinesMapping()

    /** 对位原生 Position 的行列比较；列按 UTF-8 字节数计算，fileID 不参与排序。 */
    fun nativePosition(offset: Int): Pair<Int, Int> {
        require(offset in 0..text.length)
        val line = linesMapping.getLineByOffset(offset)
        val lineStart = linesMapping.getLineStartOffset(line)
        return line + 1 to text.substring(lineStart, offset).toByteArray(Charsets.UTF_8).size + 1
    }

    /** 规范诊断范围使用同一快照的行正文，保留读取时的编码与换行。 */
    fun lineContent(line: Int): String {
        require(line in 1..linesMapping.linesCount)
        val start = linesMapping.getLineStartOffset(line - 1)
        val end = if (line < linesMapping.linesCount) linesMapping.getLineStartOffset(line) else text.length
        return text.substring(start, end).trimEnd('\r', '\n')
    }
}

/** parser 输入切片到全文的显式定位；SDK tree 的 offsetOrigin 不属于这个坐标系。 */
data class CjParserSourceContext(
    val snapshot: CjParsingSourceSnapshot,
    val inputStart: Int,
    val inputEnd: Int = snapshot.text.length,
) {
    init {
        require(inputStart in 0..inputEnd && inputEnd <= snapshot.text.length)
    }

    fun requireInput(text: CharSequence) {
        require(inputStart + text.length == inputEnd &&
            snapshot.text.regionMatches(inputStart, text.toString(), 0, text.length)) {
            "A parser source context must describe its exact input slice."
        }
    }

    companion object {
        fun complete(text: CharSequence, sourceFile: CjSourceFile? = null): CjParserSourceContext =
            CjParserSourceContext(CjParsingSourceSnapshot(sourceFile?.name, sourceFile?.path, text.toString()), 0)
    }
}

/** 主位置与每个辅助位置各自引用自己的全文快照，不借用被检查声明的 source。 */
data class CjParsingSourceRange(
    val source: CjParsingSourceSnapshot,
    val startOffset: Int,
    val endOffset: Int,
) {
    init {
        require(startOffset in 0..endOffset && endOffset <= source.text.length)
    }
}

/** 首次文法捕获的来源，移动或绑定真实 AST 时保留原事务与原位置。 */
data class CjParsedSourceOrigin(val rootIdentity: UUID, val range: CjParsingSourceRange)

/** 修饰符的原生身份、首次来源和当前已证明的源码位置；不保存角色检查后的有效属性。 */
data class CjModifierSourceFact(
    val kind: CjModifierKind,
    val origin: CjParsedSourceOrigin,
    val sourceRange: CjParsingSourceRange = origin.range,
)

/** 内建或 custom 身份由原注解文法确定；后续片段不根据名字或所属模块重新分类。 */
data class CjAnnotationSourceFact(
    val sourceName: String,
    val kind: org.cangnova.cangjie.annotations.BuiltInAnnotationKind?,
    val origin: CjParsedSourceOrigin,
    val sourceRange: CjParsingSourceRange = origin.range,
)
