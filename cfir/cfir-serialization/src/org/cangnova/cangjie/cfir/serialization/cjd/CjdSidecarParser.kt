package org.cangnova.cangjie.cfir.serialization.cjd

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
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
        val tree = CangJieLightParser.parse(builder, sourceKind = CjSourceKind.DECLARATION)
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
        return CjdSyntaxNode(node.elementType, node.startOffset, node.startOffset + node.textLength, children,
            if (node.elementType == TokenType.ERROR_ELEMENT) (node.psi as? com.intellij.psi.PsiErrorElement)?.errorDescription else null)
    }

    private fun snapshot(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): CjdSyntaxNode {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val nodes = ref.get()
        val children = if (nodes == null) emptyList() else (0 until count).map { snapshot(checkNotNull(nodes[it]), tree) }
        if (nodes != null) tree.disposeChildren(nodes, count)
        return CjdSyntaxNode(node.tokenType, node.startOffset, node.endOffset, children,
            if (node.tokenType == TokenType.ERROR_ELEMENT) PsiBuilderImpl.getErrorMessage(node) else null)
    }
}

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
