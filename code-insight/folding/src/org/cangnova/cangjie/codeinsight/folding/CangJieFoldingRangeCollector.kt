package org.cangnova.cangjie.codeinsight.folding

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.lexer.cdoc.lexer.CDocTokens
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjImportList
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.psiUtil.endLine
import org.cangnova.cangjie.psi.psiUtil.endOffset
import org.cangnova.cangjie.psi.psiUtil.referenceExpression
import org.cangnova.cangjie.psi.psiUtil.siblings
import org.cangnova.cangjie.psi.psiUtil.startLine
import org.cangnova.cangjie.psi.psiUtil.startOffset
import org.cangnova.cangjie.psi.stubs.elements.CjFunctionElementType

/**
 * 仓颉代码折叠规则收集器。
 *
 * 该类只产出与宿主无关的折叠区域；IDE 与 LSP 分别负责把区域转换为各自协议对象。
 */
object CangJieFoldingRangeCollector {
    fun collect(file: CjFile, document: Document): List<CangJieFoldingRegion> {
        val regions = mutableListOf<CangJieFoldingRegion>()
        collectImports(file, regions)
        appendRegions(file.node, document, regions)
        return regions
    }

    /**
     * 返回与 IDE 既有 `CangJieFoldingBuilder` 一致的占位文本。
     */
    fun placeholderText(node: ASTNode): String = when {
        node.elementType == CjTokens.BLOCK_COMMENT -> "/${getFirstLineOfComment(node)}.../"
        node.elementType == CDocTokens.CDOC -> "/**${getFirstLineOfComment(node)}...*/"
        node.elementType == CjNodeTypes.STRING_TEMPLATE -> "\"\"\"${getTrimmedFirstLineOfString(node).addSpaceIfNeeded()}...\"\"\""
        node.elementType == CjNodeTypes.PRIMARY_CONSTRUCTOR || node.elementType == CjNodeTypes.CALL_EXPRESSION -> "(...)"
        node.psi is CjImportList -> "..."
        else -> "{...}"
    }

    private fun collectImports(file: CjFile, regions: MutableList<CangJieFoldingRegion>) {
        val importList = file.importList ?: return
        val firstImport = importList.imports.firstOrNull() ?: return
        if (importList.imports.size <= 1) return

        val importKeyword = firstImport.firstChild ?: return
        regions.add(
            CangJieFoldingRegion(
                element = importList,
                range = TextRange(importKeyword.endOffset + 1, importList.endOffset),
                kind = CangJieFoldingKind.IMPORTS,
                placeholderText = "...",
                canBeRemovedWhenCollapsed = true,
            ),
        )
    }

    private fun appendRegions(
        node: ASTNode,
        document: Document,
        regions: MutableList<CangJieFoldingRegion>,
    ) {
        if (needFolding(node, document)) {
            val textRange = getRangeToFold(node, document)
            val relativeRange = textRange.shiftRight(-node.textRange.startOffset)
            val foldRegionText = node.chars.subSequence(relativeRange.startOffset, relativeRange.endOffset)
            if (StringUtil.countNewLines(foldRegionText) > 0) {
                regions.add(
                    CangJieFoldingRegion(
                        element = node.psi,
                        range = textRange,
                        kind = kindOf(node),
                        placeholderText = placeholderText(node),
                        canBeRemovedWhenCollapsed = false,
                    ),
                )
            }
        }

        var child = node.firstChildNode
        while (child != null) {
            appendRegions(child, document, regions)
            child = child.treeNext
        }
    }

    private fun needFolding(node: ASTNode, document: Document): Boolean {
        val type = node.elementType
        val parentType = node.treeParent?.elementType

        if (type is CjFunctionElementType) {
            val bodyExpression = (node.psi as? CjNamedFunction)?.bodyExpression
            if (bodyExpression != null && bodyExpression !is CjBlockExpression) return true
        }

        return type == CjNodeTypes.FUNCTION_LITERAL ||
                (type == CjNodeTypes.BLOCK && parentType != CjNodeTypes.FUNCTION_LITERAL) ||
                type == CjNodeTypes.CLASS_BODY ||
                type == CjTokens.BLOCK_COMMENT ||
                type == CDocTokens.CDOC ||
                type == CjNodeTypes.STRING_TEMPLATE ||
                type == CjNodeTypes.PRIMARY_CONSTRUCTOR ||
                type == CjNodeTypes.MATCH ||
                node.shouldFoldCall(document)
    }

    private fun ASTNode.shouldFoldCall(document: Document): Boolean {
        val call = (psi as? CjCallExpression)?.takeUnless { it.valueArguments.size < 2 } ?: return false
        if (call.startLine(document) == call.endLine(document)) return false
        return call.referenceExpression() != null
    }

    private fun getRangeToFold(node: ASTNode, document: Document): TextRange {
        when (node.elementType) {
            is CjFunctionElementType -> {
                val function = node.psi as? CjNamedFunction
                val funKeyword = function?.funKeyword
                val bodyExpression = function?.bodyExpression
                if (funKeyword != null && bodyExpression != null && bodyExpression !is CjBlockExpression) {
                    if (funKeyword.startLine(document) != bodyExpression.startLine(document)) {
                        val lineBreak = bodyExpression.siblings(forward = false, withItself = false).firstOrNull { "\n" in it.text }
                        if (lineBreak != null) {
                            return TextRange(lineBreak.startOffset, bodyExpression.endOffset)
                        }
                    }
                    return bodyExpression.textRange
                }
            }

            CjNodeTypes.FUNCTION_LITERAL -> {
                val psi = node.psi as? CjFunctionLiteral
                val lbrace = psi?.lBrace
                val rbrace = psi?.rBrace
                if (lbrace != null && rbrace != null) {
                    return TextRange(lbrace.startOffset, rbrace.endOffset)
                }
            }

            CjNodeTypes.CALL_EXPRESSION -> {
                val valueArgumentList = (node.psi as? CjCallExpression)?.valueArgumentList
                val leftParenthesis = valueArgumentList?.leftParenthesis
                val rightParenthesis = valueArgumentList?.rightParenthesis
                if (leftParenthesis != null && rightParenthesis != null) {
                    return TextRange(leftParenthesis.startOffset, rightParenthesis.endOffset)
                }
            }

            CjNodeTypes.MATCH -> {
                val matchExpression = node.psi as? CjMatchExpression
                val openBrace = matchExpression?.openBrace
                val closeBrace = matchExpression?.closeBrace
                if (openBrace != null && closeBrace != null) {
                    return TextRange(openBrace.startOffset, closeBrace.endOffset)
                }
            }
        }

        return node.textRange
    }

    private fun kindOf(node: ASTNode): CangJieFoldingKind {
        return when (node.elementType) {
            CjTokens.BLOCK_COMMENT, CDocTokens.CDOC -> CangJieFoldingKind.COMMENT
            else -> CangJieFoldingKind.REGION
        }
    }

    private fun getCommentContents(line: String): String {
        return line.trim()
            .removePrefix("/**")
            .removePrefix("/*")
            .removePrefix("*/")
            .removePrefix("*")
            .trim()
    }

    private fun getFirstLineOfComment(node: ASTNode): String {
        val targetCommentLine = node.text.split("\n").firstOrNull {
            getCommentContents(it).isNotEmpty()
        } ?: return ""
        return " ${getCommentContents(targetCommentLine)} "
    }

    private fun getTrimmedFirstLineOfString(node: ASTNode): String {
        val lines = node.text.split("\n")
        val firstLine = lines.asSequence().map { it.replace("\"\"\"", "").trim() }.firstOrNull(String::isNotEmpty)
        return firstLine ?: ""
    }

    private fun String.addSpaceIfNeeded(): String {
        if (isEmpty() || endsWith(" ")) return this
        return "$this "
    }
}
