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
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
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

/**
 * typealias 声明级诊断位置：从 `type` 关键字到名称/类型参数列表结束。
 *
 * Kotlin FIR 的 typealias checker 直接在 declaration source 上报告；本仓颉前端需要
 * 明确收窄到声明头部，避免包含右侧展开类型导致 LLT 范围不稳定。
 */
internal fun CfirTypeAlias.typeAliasDeclarationHeaderDiagnosticSource(): AbstractCjSourceElement? {
    val declarationSource = source ?: return null
    source?.psi?.let { psi ->
        val typeAliasPsi = when (psi) {
            is CjTypeAlias -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjTypeAlias::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjTypeAlias::class.java)
        }
        val keyword = typeAliasPsi?.getTypeAliasKeyword()
        val endElement = typeAliasPsi?.typeParameterList ?: typeAliasPsi?.nameIdentifier
        if (keyword != null && endElement != null) {
            return CjOffsetsOnlySourceElement(
                startOffset = keyword.textRange.startOffset,
                endOffset = endElement.textRange.endOffset,
            )
        }
    }
    return (declarationSource as? CjSourceElement)?.findTypeAliasHeaderSource(name) ?: declarationSource
}

/**
 * 官方 cjc 的部分声明级诊断锚定在声明节点起始位置，JSON 主范围只有首字符。
 * 使用 offsets-only source 避免 PSI 默认范围扩展到整条声明。
 */
internal fun AbstractCjSourceElement.firstCharacterDiagnosticSource(): AbstractCjSourceElement =
    CjOffsetsOnlySourceElement(
        startOffset = startOffset,
        endOffset = (startOffset + 1).coerceAtMost(endOffset),
    )

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

internal fun CfirConstructor.constructorNameDiagnosticSource(
    includeConstKeyword: Boolean = false,
): AbstractCjSourceElement? =
    source?.psi?.let { psi ->
        val constructorPsi = when (psi) {
            is CjConstructor<*> -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjConstructor::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjConstructor::class.java)
        }
        val initKeyword = constructorPsi?.getInitKeyword()
        if (initKeyword != null) {
            if (includeConstKeyword) {
                val constKeyword = constructorPsi.modifierList?.getModifier(CjTokens.CONST_KEYWORD)
                    ?: constructorPsi.node.findChildByType(CjTokens.CONST_KEYWORD)?.psi
                if (constKeyword != null && constKeyword.textRange.startOffset <= initKeyword.textRange.startOffset) {
                    return CjOffsetsOnlySourceElement(
                        startOffset = constKeyword.textRange.startOffset,
                        endOffset = initKeyword.textRange.endOffset,
                    )
                }
            }
            return initKeyword.toCjPsiSourceElement()
        }
        constructorPsi?.nameIdentifier?.toCjPsiSourceElement()
    }
        ?: (source as? CjSourceElement)?.findConstructorNameSource(includeConstKeyword)
        ?: source

internal fun CfirFieldVariable.fieldVariableNameDiagnosticSource(): AbstractCjSourceElement? =
    source?.psi?.let { psi ->
        val fieldPsi = when (psi) {
            is CjFieldVariable -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjFieldVariable::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjFieldVariable::class.java)
        }
        fieldPsi?.nameIdentifier?.toCjPsiSourceElement()
    }
        ?: (source as? CjSourceElement)?.findFieldVariableNameSource(name)
        ?: source

internal fun CfirProperty.propertyNameDiagnosticSource(): AbstractCjSourceElement? =
    source?.psi?.let { psi ->
        val propertyPsi = when (psi) {
            is CjProperty -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjProperty::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjProperty::class.java)
        }
        propertyPsi?.nameIdentifier?.toCjPsiSourceElement()
    }
        ?: (source as? CjSourceElement)?.findPropertyNameSource(name)
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

private fun CjSourceElement.findTypeAliasHeaderSource(name: Name): AbstractCjSourceElement? {
    val tokens = collectLeafTokens()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType != CjTokens.TYPE_KEYWORD) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .takeWhile { it.tokenType != CjTokens.EQ && it.tokenType != CjTokens.LBRACE }
            .firstOrNull { it.tokenType == CjTokens.IDENTIFIER && treeStructure.toString(it).toString() == name.asString() }
            ?: continue
        val endToken = tokens.asSequence()
            .drop(tokens.indexOf(nameToken) + 1)
            .takeWhile { it.tokenType != CjTokens.EQ && it.tokenType != CjTokens.LBRACE }
            .filter { treeStructure.toString(it).toString().isNotBlank() }
            .lastOrNull()
            ?.takeIf { treeStructure.getEndOffset(it) > treeStructure.getEndOffset(nameToken) }
            ?: nameToken
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(token),
            endOffset = treeStructure.getEndOffset(endToken),
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

private fun CjSourceElement.findFieldVariableNameSource(name: Name): AbstractCjSourceElement? {
    val tokens = collectSourceNodes()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType !in fieldVariableDeclarationKeywords) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .takeWhile { it.tokenType != CjTokens.COLON && it.tokenType != CjTokens.EQ && it.tokenType != CjTokens.LBRACE }
            .firstOrNull { node ->
                node.tokenType in fieldVariableNameTokenTypes &&
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

private fun CjSourceElement.findPropertyNameSource(name: Name): AbstractCjSourceElement? {
    val tokens = collectSourceNodes()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType != CjTokens.PROP_KEYWORD) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .takeWhile { it.tokenType != CjTokens.COLON && it.tokenType != CjTokens.LBRACE }
            .firstOrNull { node ->
                node.tokenType in propertyNameTokenTypes &&
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

private fun CjSourceElement.findConstructorNameSource(
    includeConstKeyword: Boolean,
): AbstractCjSourceElement? {
    val tokens = collectLeafTokens()

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType != CjTokens.INIT_KEYWORD) continue
        val startToken = if (includeConstKeyword) {
            tokens.asSequence()
                .take(index)
                .lastOrNull { it.tokenType == CjTokens.CONST_KEYWORD }
                ?: token
        } else {
            token
        }
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(startToken),
            endOffset = treeStructure.getEndOffset(token),
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

private val fieldVariableDeclarationKeywords = setOf(
    CjTokens.LET_KEYWORD,
    CjTokens.CONST_KEYWORD,
    CjTokens.VAR_KEYWORD,
)

private val fieldVariableNameTokenTypes = setOf(
    CjTokens.IDENTIFIER,
    CjTokens.FIELD_IDENTIFIER,
)

private val propertyNameTokenTypes = setOf(
    CjTokens.IDENTIFIER,
    CjTokens.FIELD_IDENTIFIER,
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
