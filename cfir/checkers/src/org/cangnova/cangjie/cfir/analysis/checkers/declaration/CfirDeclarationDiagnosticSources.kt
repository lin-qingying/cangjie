package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjOperationName
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.psi.CjClassLikeDeclaration as CjPsiClassLikeDeclaration

/**
 * CFIR checker 统一使用的声明名称诊断位置。
 *
 * 宏构造后的声明可能没有 PSI，且声明 source 可能覆盖注解与声明整体；
 * checker 报错需要稳定落到声明名或函数名，不能依赖 PSI fallback。
 */
internal fun CfirClassLikeDeclaration.classLikeNameDiagnosticSource(
    includeTypeParameters: Boolean = false,
): AbstractCjSourceElement? {
    source?.psi?.let { psi ->
        val classLikePsi = when (psi) {
            is CjPsiClassLikeDeclaration -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjPsiClassLikeDeclaration::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjPsiClassLikeDeclaration::class.java)
        }
        classLikePsi?.nameIdentifier?.toCjPsiSourceElement()?.let { return it }
    }
    return (source as? CjSourceElement)?.findClassLikeNameSource(symbol.name, includeTypeParameters) ?: source
}

internal fun CfirNamedFunction.functionNameDiagnosticSource(): AbstractCjSourceElement? =
    source?.psi?.let { psi ->
        val functionPsi = when (psi) {
            is CjNamedFunction -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjNamedFunction::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjNamedFunction::class.java)
        }
        val nameElement = functionPsi?.nameIdentifier
            ?: functionPsi?.let { PsiTreeUtil.findChildOfType(it, CjOperationName::class.java) }
        nameElement?.toCjPsiSourceElement()
    }
        ?: (source as? CjSourceElement)?.findFunctionNameSource(name)
        ?: source

private fun CjSourceElement.findClassLikeNameSource(
    name: Name,
    includeTypeParameters: Boolean,
): AbstractCjSourceElement? {
    val tokens = collectLeafTokens()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType !in classLikeDeclarationKeywords) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .firstOrNull { it.tokenType == CjTokens.IDENTIFIER && treeStructure.toString(it).toString() == name.asString() }
            ?: continue
        if (includeTypeParameters) {
            val endToken = tokens.asSequence()
                .drop(tokens.indexOf(nameToken) + 1)
                .takeWhile { it.tokenType != CjTokens.LTCOLON && it.tokenType != CjTokens.LBRACE }
                .filter { treeStructure.toString(it).toString().isNotBlank() }
                .lastOrNull()
            if (endToken != null && treeStructure.getEndOffset(endToken) > treeStructure.getEndOffset(nameToken)) {
                return CjOffsetsOnlySourceElement(
                    startOffset = treeStructure.getStartOffset(nameToken),
                    endOffset = treeStructure.getEndOffset(endToken),
                )
            }
        }
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(nameToken),
            endOffset = treeStructure.getEndOffset(nameToken),
        )
    }

    return null
}

private fun CjSourceElement.findFunctionNameSource(name: Name): AbstractCjSourceElement? {
    val tokens = collectSourceNodes()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType != CjTokens.FUNC_KEYWORD) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .takeWhile { it.tokenType != CjTokens.LPAR && it.tokenType != CjTokens.COLON && it.tokenType != CjTokens.LBRACE }
            .firstOrNull { node ->
                node.tokenType in functionNameTokenTypes &&
                    treeStructure.toString(node).toString() == name.asString()
            }
            ?: continue
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(nameToken),
            endOffset = treeStructure.getEndOffset(nameToken),
        )
    }

    return null
}

private val functionNameTokenTypes = setOf(
    CjTokens.IDENTIFIER,
    CjNodeTypes.OPERATION_NAME,
)

private fun CjSourceElement.collectSourceNodes(): List<LighterASTNode> {
    val nodes = mutableListOf<LighterASTNode>()

    fun collect(node: LighterASTNode) {
        nodes += node
        treeStructure.children(node).forEach(::collect)
    }

    collect(lighterASTNode)
    return nodes
}

private fun CjSourceElement.collectLeafTokens(): List<LighterASTNode> {
    val tokens = mutableListOf<LighterASTNode>()

    fun collectLeaves(node: LighterASTNode) {
        val children = treeStructure.children(node)
        if (children.isEmpty()) {
            tokens += node
            return
        }
        children.forEach(::collectLeaves)
    }

    collectLeaves(lighterASTNode)
    return tokens
}

private val classLikeDeclarationKeywords = setOf(
    CjTokens.CLASS_KEYWORD,
    CjTokens.STRUCT_KEYWORD,
    CjTokens.INTERFACE_KEYWORD,
    CjTokens.ENUM_KEYWORD,
)

private fun FlyweightCapableTreeStructure<LighterASTNode>.children(
    node: LighterASTNode,
): List<LighterASTNode> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.filterNotNull().orEmpty()
}
