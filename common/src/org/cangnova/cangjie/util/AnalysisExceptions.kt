package org.cangnova.cangjie.util

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

val Throwable.classNameAndMessage get() = "${this::class.qualifiedName}: $message"

class SourceCodeAnalysisException(val source: CjSourceElement, override val cause: Throwable) : Exception() {
    override val message get() = cause.classNameAndMessage
}
fun Throwable.wrapIntoSourceCodeAnalysisExceptionIfNeeded(element: CjSourceElement?): Throwable =
    if (this is SourceCodeAnalysisException || shouldIjPlatformExceptionBeRethrown(this) || this is VirtualMachineError) {
        this
    } else {
        when (element?.kind) {
            is CjRealSourceElementKind -> SourceCodeAnalysisException(element, this)
            else -> this
        }
    }

class FileAnalysisException(
    private val path: String,
    override val cause: Throwable,
    private val lineAndOffset: Pair<Int, Int>? = null,
) : Exception() {
    override val message
        get(): String {
            val (line, offset) = lineAndOffset ?: return "Somewhere in file $path: ${cause.classNameAndMessage}"
            return "While analysing $path:${line + 1}:${offset + 1}: ${cause.classNameAndMessage}"
        }
}

fun Throwable.wrapIntoFileAnalysisExceptionIfNeeded(
    filePath: String?,
    fileSource: CjSourceElement?,
    linesMapping: (Int) -> Pair<Int, Int>?,
) = when {
    filePath == null || fileSource == null -> when (this) {
        is SourceCodeAnalysisException -> error("Sourceless FirFile contains a FirElement with a real source element")
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
private val CharSequence.asQuote: String
    get() = split("\n").joinToString("\n") { "> $it" }

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

private fun LighterASTNode.isInside(other: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    return generateSequence(tree.getParent(this)) { tree.getParent(it) }.any { it == other }
}

private fun CjSourceElement.isDefinitelyNotInsideFile(fileSource: CjSourceElement): Boolean {
    val thisPsi = psi
    val otherPsi = fileSource.psi

    return when {
        thisPsi != null && otherPsi != null -> thisPsi.containingFile != otherPsi
        else -> !lighterASTNode.isInside(fileSource.lighterASTNode, treeStructure)
    }
}
