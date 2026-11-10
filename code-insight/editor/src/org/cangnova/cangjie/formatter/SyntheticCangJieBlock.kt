/*
 * Copyright 2025 LinQingYing. and contributors.
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


package org.cangnova.cangjie.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange

/**
 * formatter 为一组合并子 block 构造的合成 AST block。
 */
class SyntheticCangJieBlock(
    /** 合成 block 对应的父 AST 节点。 */
    private val node: ASTNode,
    /** 合成 block 包含的实际子 block。 */
    private val subBlocks: List<ASTBlock>,
    /** 合成 block 使用的对齐对象。 */
    private val alignment: Alignment?,
    /** 合成 block 使用的缩进。 */
    private val indent: Indent?,
    /** 合成 block 使用的 wrap。 */
    private val wrap: Wrap?,
    /** spacing 计算使用的仓颉 spacing builder。 */
    private val spacingBuilder: CangJieSpacingBuilder,
    /** 创建父级合成 spacing block 的回调。 */
    private val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock
) : ASTBlock {

    /**
     * 合成 block 覆盖的整体文本范围。
     */
    private val textRange = TextRange(
        subBlocks.first().textRange.startOffset,
        subBlocks.last().textRange.endOffset
    )

    /**
     * 返回合成 block 文本范围。
     */
    override fun getTextRange(): TextRange = textRange
    /**
     * 返回合成 block 的实际子 block。
     */
    override fun getSubBlocks() = subBlocks
    /**
     * 返回合成 block 的 wrap。
     */
    override fun getWrap() = wrap
    /**
     * 返回合成 block 的缩进。
     */
    override fun getIndent() = indent
    /**
     * 返回合成 block 的对齐对象。
     */
    override fun getAlignment() = alignment
    /**
     * 返回新子节点的默认缩进属性。
     */
    override fun getChildAttributes(newChildIndex: Int) = ChildAttributes(getIndent(), null)
    /**
     * 合成 block 的不完整状态由最后一个子 block 决定。
     */
    override fun isIncomplete() = getSubBlocks().last().isIncomplete
    /**
     * 合成 block 总是包含子 block。
     */
    override fun isLeaf() = false
    /**
     * 返回合成 block 对应的 AST 节点。
     */
    override fun getNode() = node
    /**
     * 使用父级合成 spacing block 计算子 block 间距。
     */
    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(createParentSyntheticSpacingBlock(node), child1, child2)
    }


    /**
     * 返回合成 block 覆盖文本和范围，便于 formatter 调试。
     */
    override fun toString(): String {
        var child = subBlocks.first()
        var treeNode: ASTNode? = null

        loop@
        while (treeNode == null) when (child) {
            is SyntheticCangJieBlock -> child = child.getSubBlocks().first()

            else -> treeNode = child.node
        }

        val textRange = getTextRange()
        val psi = treeNode.psi
        if (psi != null) {
            val file = psi.containingFile
            if (file != null) {
                return file.text!!.subSequence(textRange.startOffset, textRange.endOffset).toString() + " " + textRange
            }
        }

        return this::class.java.name + ": " + textRange
    }
}
