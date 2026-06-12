/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjOperationName
import org.cangnova.cangjie.source.*
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

/**
 * 类型声明头部诊断位置：从声明起点到声明名（含类型参数）结束。
 *
 * 官方 cjc 对部分类型声明诊断使用 Decl/Node 起点作为主位置；LLT inline
 * 需要稳定范围时，使用声明头部而不是 Kotlin 的 name-only 位置。
 */
internal fun CfirClassLikeDeclaration.classLikeDeclarationHeaderDiagnosticSource(): AbstractCjSourceElement? {
    val declarationSource = source ?: return null
    val nameSource = classLikeNameDiagnosticSource(includeTypeParameters = true) ?: return declarationSource
    return CjOffsetsOnlySourceElement(
        startOffset = declarationSource.startOffset,
        endOffset = nameSource.endOffset,
    )
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

internal fun CfirFinalizer.finalizerNameDiagnosticSource(): AbstractCjSourceElement? =
    source?.psi?.let { psi ->
        val finalizerPsi = when (psi) {
            is CjFinalizer -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjFinalizer::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjFinalizer::class.java)
        }
        val tilde = finalizerPsi?.node?.findChildByType(CjTokens.TILDE)?.psi
        val init = finalizerPsi?.initKeyword
        if (tilde != null && init != null) {
            CjOffsetsOnlySourceElement(
                startOffset = tilde.textRange.startOffset,
                endOffset = init.textRange.endOffset,
            )
        } else {
            init?.toCjPsiSourceElement()
        }
    }
        ?: (source as? CjSourceElement)?.findFinalizerNameSource()
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

private fun CjSourceElement.findFinalizerNameSource(): AbstractCjSourceElement? {
    val tokens = collectLeafTokens()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType != CjTokens.TILDE) continue
        val initToken = tokens.asSequence()
            .drop(index + 1)
            .firstOrNull { it.tokenType == CjTokens.INIT_KEYWORD }
            ?: continue
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(token),
            endOffset = treeStructure.getEndOffset(initToken),
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
