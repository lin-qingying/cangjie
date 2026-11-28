package org.cangnova.cangjie.cfir.serialization.cjd

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CjParsingErrorElementType
import org.cangnova.cangjie.parsing.isParsingErrorElementType
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjSourceRangeErrorElement
import org.cangnova.cangjie.psi.CjTokenRangeErrorElement
import org.cangnova.cangjie.psi.CjFile
import java.nio.file.Files
import java.nio.file.Path

/**
 * 仅语法解析的 sidecar 服务。调用方须先初始化 IntelliJ application/parser 基础设施。
 * PSI 入口须在调用方的读操作中调用；不创建项目、session、CFIR，不运行 Sema 或宏。
 * 磁盘 I/O 异常向调用方传播；可选文件的缺失由 locator/加载层处理。
 */
interface CjdSidecarParser {
    fun parse(path: Path): CjdSidecarIndex
    fun parse(file: CjFile): CjdSidecarIndex

    companion object {
        fun create(): CjdSidecarParser = SyntaxCjdSidecarParser()
    }
}

/** [CjdSidecarParser] 的默认实现；分别支持磁盘路径与 PSI 两种入口的纯语法解析。 */
private class SyntaxCjdSidecarParser : CjdSidecarParser {
    override fun parse(path: Path): CjdSidecarIndex {
        val text = Files.readString(path)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val tree = CangJieLightParser.parse(builder, sourceKind = CjSourceKind.DECLARATION).tree
        return CjdStructureExtractor(text).extract(snapshot(tree.root, tree), path.toString(), path)
    }

    override fun parse(file: CjFile): CjdSidecarIndex {
        require(file.sourceKind == CjSourceKind.DECLARATION) { "Sidecar PSI must be a declaration file" }
        return CjdStructureExtractor(file.text).extract(snapshot(file.node), file.virtualFile?.path ?: file.name, null)
    }

    private fun snapshot(node: ASTNode): CjdSyntaxNode {
        // 文档注释有自己的惰性 parser；其中的错误不属于声明语法。
        val children = if (CjTokens.COMMENTS.contains(node.elementType)) emptyList()
            else generateSequence(node.firstChildNode) { it.treeNext }.map(::snapshot).toList()
        val startOffset = node.startOffset
        val endOffset = startOffset + node.textLength
        val error = if (isParserErrorType(node.elementType)) {
            parserError(node.psi as PsiErrorElement, startOffset, endOffset)
        } else {
            null
        }
        return CjdSyntaxNode(
            error?.let { TokenType.ERROR_ELEMENT } ?: node.elementType,
            error?.range?.startOffset ?: startOffset,
            error?.range?.endOffset ?: endOffset,
            children,
            error?.message,
        )
    }

    private fun snapshot(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): CjdSyntaxNode {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val nodes = ref.get()
        val children = if (nodes == null) emptyList() else (0 until count).map { snapshot(checkNotNull(nodes[it]), tree) }
        if (nodes != null) tree.disposeChildren(nodes, count)
        val message = if (node.tokenType == TokenType.ERROR_ELEMENT) PsiBuilderImpl.getErrorMessage(node) else null
        val error = parserError(node.tokenType, node.startOffset, node.endOffset, message)
        return CjdSyntaxNode(
            error?.let { TokenType.ERROR_ELEMENT } ?: node.tokenType,
            error?.range?.startOffset ?: node.startOffset,
            error?.range?.endOffset ?: node.endOffset,
            children,
            error?.message,
        )
    }

    /** 自定义 parser 错误节点本身通常为零宽；sidecar 必须保存其协议定义的报告范围。 */
    private fun parserError(
        errorElement: PsiErrorElement,
        nodeStart: Int,
        nodeEnd: Int,
    ): CjdParserError {
        val reportedRange = when (errorElement) {
            is CjTokenRangeErrorElement -> errorElement.reportedRange
            is CjSourceRangeErrorElement -> errorElement.reportedRange
            else -> null
        }
        val range = reportedRange?.let { CjdSourceRange(it.startOffset, it.endOffset) }
            ?: CjdSourceRange(nodeStart, nodeEnd)
        return CjdParserError(range, errorElement.errorDescription)
    }

    private fun isParserErrorType(kind: IElementType): Boolean = kind.isParsingErrorElementType()

    private fun parserError(
        kind: IElementType,
        nodeStart: Int,
        nodeEnd: Int,
        message: String?,
    ): CjdParserError? = when (kind) {
        TokenType.ERROR_ELEMENT -> CjdParserError(CjdSourceRange(nodeStart, nodeEnd), checkNotNull(message))
        is CjParsingErrorElementType -> {
            val diagnostic = kind.diagnostic(nodeStart, nodeEnd)
            CjdParserError(CjdSourceRange(diagnostic.startOffset, diagnostic.endOffset), diagnostic.message)
        }
        else -> null
    }
}

private data class CjdParserError(val range: CjdSourceRange, val message: String)

/** 两种树入口共享的内部结构视图，生命周期仅限一次提取。 */
internal data class CjdSyntaxNode(
    val kind: IElementType,
    val start: Int,
    val end: Int,
    val children: List<CjdSyntaxNode>,
    val error: String? = null,
) {
    val range: CjdSourceRange get() = CjdSourceRange(start, end)
    fun child(kind: IElementType): CjdSyntaxNode? = children.firstOrNull { it.kind == kind }
    fun children(kind: IElementType): List<CjdSyntaxNode> = children.filter { it.kind == kind }
}
