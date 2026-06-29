package org.cangnova.cangjie.source

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import java.io.InputStreamReader

/**
 * 源文件行列号与文本偏移之间的映射接口。
 */
interface CjSourceFileLinesMapping {
    /**
     * 返回指定行的起始文本偏移。
     */
    fun getLineStartOffset(line: Int): Int
    /**
     * 将文本偏移转换为行号和列号。
     */
    fun getLineAndColumnByOffset(offset: Int): Pair<Int, Int>
    /**
     * 返回指定文本偏移所在的行号。
     */
    fun getLineByOffset(offset: Int): Int

    /**
     * 文件最后一个可用文本偏移。
     */
    val lastOffset: Int
    /**
     * 文件总行数。
     */
    val linesCount: Int
}
/**
 * 基于 PSI 文件 document 的行列映射。
 */
class CjPsiSourceFileLinesMapping(
    /**
     * 提供 document 的 PSI 文件。
     */
    val psiFile: PsiFile,
) : CjSourceFileLinesMapping {
    /**
     * PSI 文件关联的编辑器 document。
     */
    private val document: Document? by lazy { psiFile.viewProvider.document }

    /**
     * 返回指定行的 document 起始偏移。
     */
    override fun getLineStartOffset(line: Int): Int =
        document?.getLineStartOffset(line) ?: -1

    /**
     * 通过 document 将偏移转换为行列。
     */
    override fun getLineAndColumnByOffset(offset: Int): Pair<Int, Int> =
        document?.let {
            val lineNumber = it.getLineNumber(offset)
            val lineStartOffset = it.getLineStartOffset(lineNumber)
            lineNumber to offset - lineStartOffset
        } ?: (-1 to -1)

    /**
     * 通过 document 返回偏移所在行号。
     */
    override fun getLineByOffset(offset: Int): Int =
        document?.getLineNumber(offset) ?: -1

    /**
     * document 的文本长度。
     */
    override val lastOffset: Int
        get() = document?.textLength ?: -1

    /**
     * document 的行数。
     */
    override val linesCount: Int
        get() = document?.lineCount ?: 0
}

/**
 * 基于预计算行起始偏移数组的行列映射。
 */
open class CjSourceFileLinesMappingFromLineStartOffsets(
    /**
     * 每一行的起始文本偏移。
     */
    val lineStartOffsets: IntArray,
    /**
     * 文件最后一个可用文本偏移。
     */
    override val lastOffset: Int,
) : CjSourceFileLinesMapping {
    /**
     * 从预计算数组读取指定行的起始偏移。
     */
    override fun getLineStartOffset(line: Int): Int = lineStartOffsets[line]

    /**
     * 根据预计算行起始偏移将文本偏移转换为行列。
     */
    override fun getLineAndColumnByOffset(offset: Int): Pair<Int, Int> {
        val lineNumber = getLineByOffset(offset)
        if (lineNumber < 0) return -1 to -1
        val lineStartOffset = lineStartOffsets[lineNumber]
        return lineNumber to offset - lineStartOffset
    }

    /**
     * 使用二分查找返回偏移所在行号。
     */
    override fun getLineByOffset(offset: Int): Int {
        val index = lineStartOffsets.binarySearch(offset)
        return if (index >= 0) index else -index - 2
    }

    /**
     * 预计算数组长度即行数。
     */
    override val linesCount: Int
        get() = lineStartOffsets.size
}

/**
 *  Reads file contents from reader, converts line separators and calculates source lines to file offsets mapping
 *
 *  Returns CjSourceFileLinesMapping and char sequence (StringBuilder to avoid premature copying) containing converted text
 *  The separators are converted similarly to the com.intellij.openapi.util.text.StringUtilRt algorithms
 */
fun InputStreamReader.readSourceFileWithMapping(): Pair<CharSequence, CjSourceFileLinesMapping> {
    val buffer = CharArray(255)
    var bufLength = -1
    var bufPos = 0
    var skipNextLf = false

    var charsRead = 0

    val lineOffsets = mutableListOf(0) // TODO: consider using implicit first line offset (needs to be handled properly in IR)
    val sb = StringBuilder()

    while (true) {
        if (bufPos >= bufLength) {
            bufLength = read(buffer)
            bufPos = 0
            if (bufLength < 0) {
                break
            }
        } else {
            val c = buffer[bufPos++]
            charsRead++
            when {
                c == '\n' && skipNextLf -> {
                    charsRead--
                    skipNextLf = false
                }
                c == '\n' || c == '\r' -> {
                    sb.append('\n')
                    lineOffsets.add(charsRead)
                    skipNextLf = c == '\r'
                }
                else -> {
                    sb.append(c)
                    skipNextLf = false
                }
            }
        }
    }

    return sb to CjSourceFileLinesMappingFromLineStartOffsets(lineOffsets.toIntArray(), charsRead)
}

/**
 * Extracts source lines to offsets mapping from text
 *
 * intended for using mainly in tests, so no care is taken about performance or possible corner cases
 */
fun CharSequence.toSourceLinesMapping(): CjSourceFileLinesMapping {
    val lineOffsets = mutableListOf(0)
    var offset = 0
    for (c in this) {
        offset++
        if (c == '\n') lineOffsets.add(offset)
    }
    return CjSourceFileLinesMappingFromLineStartOffsets(lineOffsets.toIntArray(), offset)
}
