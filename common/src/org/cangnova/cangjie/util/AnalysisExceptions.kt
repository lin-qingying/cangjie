package org.cangnova.cangjie.util

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

/**
 * Throwable 的类名和消息组合文本。
 */
val Throwable.classNameAndMessage get() = "${this::class.qualifiedName}: $message"

/**
 * 绑定到具体源码元素的分析异常。
 */
class SourceCodeAnalysisException(
    /**
     * 触发异常的源码元素。
     */
    val source: CjSourceElement,
    /**
     * 原始异常。
     */
    override val cause: Throwable,
) : Exception() {
    /**
     * 使用原始异常类名和消息作为展示文本。
     */
    override val message get() = cause.classNameAndMessage
}
/**
 * 当异常发生在真实源码元素上时，将其包装为源码分析异常。
 */
fun Throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element: CjSourceElement?): Throwable =
    if (this is SourceCodeAnalysisException || shouldIjPlatformExceptionBeRethrown(this) || this is VirtualMachineError) {
        this
    } else {
        when (element?.kind) {
            is CjRealSourceElementKind -> SourceCodeAnalysisException(element, this)
            else -> this
        }
    }

/**
 * 绑定到具体文件路径和可选行列位置的分析异常。
 */
class FileAnalysisException(
    /**
     * 文件路径。
     */
    private val path: String,
    /**
     * 原始异常。
     */
    override val cause: Throwable,
    /**
     * 源码元素映射出的行号和列偏移。
     */
    private val lineAndOffset: Pair<Int, Int>? = null,
) : Exception() {
    /**
     * 生成包含文件路径和行列信息的异常消息。
     */
    override val message
        get(): String {
            val (line, offset) = lineAndOffset ?: return "Somewhere in file $path: ${cause.classNameAndMessage}"
            return "While analysing $path:${line + 1}:${offset + 1}: ${cause.classNameAndMessage}"
        }
}

/**
 * 将源码分析异常包装为文件级分析异常，并校验异常源元素是否属于当前文件。
 */
fun Throwable.wrapIntoFileAnalysisExceptionIfNeeded(
    filePath: String?,
    fileSource: CjSourceElement?,
    linesMapping: (Int) -> Pair<Int, Int>?,
) = when {
    filePath == null || fileSource == null -> when (this) {
        is SourceCodeAnalysisException -> error("Sourceless CfirFile contains a CfirElement with a real source element")
        else -> this
    }
    this is SourceCodeAnalysisException -> when {
        fileSource == source -> FileAnalysisException(filePath, cause)
        source.isDefinitelyNotInsideFile(fileSource) -> reportFileMismatch(source, fileSource, cause)
        else -> FileAnalysisException(filePath, cause, linesMapping(source.startOffset))
    }
    shouldIjPlatformExceptionBeRethrown(this) -> this
    this is FileAnalysisException -> this
    this is VirtualMachineError -> this
    else -> FileAnalysisException(filePath, this)
}
/**
 * 将多行文本转换为引用块格式。
 */
private val CharSequence.asQuote: String
    get() = split("\n").joinToString("\n") { "> $it" }

/**
 * 构造源码元素与文件源不匹配时的诊断异常。
 */
private fun reportFileMismatch(source: CjSourceElement, fileSource: CjSourceElement, cause: Throwable): Throwable {
    val thisPsi = source.psi
    val otherPsi = fileSource.psi
    val comparison = "This:\n\n${source.text?.asQuote}\n\n...is not present in"

    val expectedFileMessage = if (thisPsi != null && otherPsi != null) {
        val actualPath = thisPsi.containingFile.virtualFile.path
        val expectedPath = otherPsi.containingFile.virtualFile.path
        "$expectedPath, but rather in $actualPath"
    } else {
        "...${fileSource.text?.asQuote}"
    }

    return IllegalStateException(
        "KtSourceElement inside a SourceCodeAnalysisException was matched against the wrong FirFile source. $comparison$expectedFileMessage",
        cause,
    )
}

/**
 * 判断当前轻量树节点是否位于另一个节点之内。
 */
private fun LighterASTNode.isInside(other: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    return generateSequence(tree.getParent(this)) { tree.getParent(it) }.any { it == other }
}

/**
 * 判断源码元素是否明确不属于指定文件源元素。
 */
private fun CjSourceElement.isDefinitelyNotInsideFile(fileSource: CjSourceElement): Boolean {
    val thisPsi = psi
    val otherPsi = fileSource.psi

    return when {
        thisPsi != null && otherPsi != null -> thisPsi.containingFile != otherPsi
        else -> !lighterASTNode.isInside(fileSource.lighterASTNode, treeStructure)
    }
}

/**
 * 执行代码块并在边界处拆除 SourceCodeAnalysisException 包装。
 */
inline fun <R> withSourceCodeAnalysisExceptionUnwrapping(block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        throw if (throwable is SourceCodeAnalysisException) throwable.cause else throwable
    }
}
