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

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes.MATCH_ENTRY
import com.intellij.formatting.*
import com.intellij.formatting.alignment.AlignmentStrategy
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType

/**
 * 仓颉 formatter 的标准 AST block 实现。
 */
class CangJieBlock(node: ASTNode,
                   /** 当前 block 的对齐策略。 */
                   private val myAlignmentStrategy: CommonAlignmentStrategy,
                   /** 当前 block 的缩进。 */
                   private val myIndent: Indent?,
                   wrap: Wrap?,
                   mySettings: CodeStyleSettings,
                   /** 当前 block 使用的 spacing builder。 */
                   private val mySpacingBuilder: CangJieSpacingBuilder,
                   overrideChildren: Sequence<ASTNode>? = null)

: AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)) {
    /**
     * 承担仓颉通用 block 构建逻辑的委托对象。
     */
    private val cangjieDelegationBlock = object : CangJieCommonBlock(
        node, mySettings, mySpacingBuilder, myAlignmentStrategy, overrideChildren
    ) {
        /**
         * 返回不产生对齐的策略。
         */
        override fun getNullAlignmentStrategy(): CommonAlignmentStrategy = NodeAlignmentStrategy.nullStrategy

        /**
         * 根据配置创建节点级对齐策略。
         */
        override fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(createAlignment(alignOption, defaultAlignment)))
        }

        /**
         * 为 match case 分支创建按列对齐策略。
         */
        override fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy {
            return if (shouldAlignInColumns) {
                NodeAlignmentStrategy.fromTypes(
                    AlignmentStrategy.createAlignmentPerTypeStrategy(listOf(CjTokens.DOUBLE_ARROW as IElementType), MATCH_ENTRY, true)
                )
            } else {
                NodeAlignmentStrategy.nullStrategy
            }
        }

        /**
         * 返回当前 block 的对齐对象。
         */
        override fun getAlignment(): Alignment? = alignment

        /**
         * 委托到外层 AbstractBlock 判断不完整状态。
         */
        override fun isIncompleteInSuper(): Boolean = super@CangJieBlock.isIncomplete()

        /**
         * 委托到外层 AbstractBlock 获取子节点属性。
         */
        override fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes = super@CangJieBlock.getChildAttributes(newChildIndex)

        /**
         * 返回外层 AbstractBlock 的子 block 列表。
         */
        override fun getSubBlocks(): List<Block> = subBlocks

        /**
         * 创建子 AST block。
         */
        override fun createBlock(
            node: ASTNode,
            alignmentStrategy: CommonAlignmentStrategy,
            indent: Indent?,
            wrap: Wrap?,
            settings: CodeStyleSettings,
            spacingBuilder: CangJieSpacingBuilder,
            overrideChildren: Sequence<ASTNode>?
        ): ASTBlock {
            return CangJieBlock(
                node,
                alignmentStrategy,
                indent,
                wrap,
                mySettings,
                mySpacingBuilder,
                overrideChildren
            )
        }

        /**
         * 创建仅用于 spacing 计算的合成节点 block。
         */
        override fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock {
            return object : AbstractBlock(node, null, null) {
                /**
                 * 合成 spacing block 不是叶子，允许参与 spacing 计算。
                 */
                override fun isLeaf(): Boolean = false
                /**
                 * 合成 spacing block 自身不提供子 spacing。
                 */
                override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
                /**
                 * 合成 spacing block 不构建子 block。
                 */
                override fun buildChildren(): List<Block> = emptyList()
            }
        }
    }

    /**
     * 返回当前 block 的缩进。
     */
    override fun getIndent(): Indent? = myIndent

    /**
     * 构建当前 block 的子 block。
     */
    override fun buildChildren(): List<Block> = cangjieDelegationBlock.buildChildren()

    /**
     * 通过仓颉 spacing builder 计算两个子 block 之间的 spacing。
     */
    override fun getSpacing(child1: Block?, child2: Block): Spacing? = mySpacingBuilder.getSpacing(this, child1, child2)

    /**
     * 返回新子节点的默认缩进属性。
     */
    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = cangjieDelegationBlock.getChildAttributes(newChildIndex)

    /**
     * 判断当前 block 是否为叶子。
     */
    override fun isLeaf(): Boolean = cangjieDelegationBlock.isLeaf()

    /**
     * 返回当前 block 的文本范围。
     */
    override fun getTextRange() = cangjieDelegationBlock.getTextRange()

    /**
     * 判断当前 block 是否语法不完整。
     */
    override fun isIncomplete(): Boolean = cangjieDelegationBlock.isIncomplete()
}

/**
 * 仓颉 formatter 使用的 spacing builder 平台适配工具。
 */
object CangJieSpacingBuilderUtilImpl : CangJieSpacingBuilderUtil {
    /**
     * 返回指定节点前一个非空白叶子节点。
     */
    override fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode? {
        return FormatterUtil.getPreviousNonWhitespaceLeaf(node)
    }

    /**
     * 判断节点是否为空白或空节点。
     */
    override fun isWhitespaceOrEmpty(node: ASTNode?): Boolean {
        return FormatterUtil.isWhitespaceOrEmpty(node)
    }

    /**
     * 创建依赖换行状态的 spacing。
     */
    override fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing {
        return Spacing.createSpacing(
            minSpaces,
            maxSpaces,
            minimumLineFeeds,
            keepLineBreaks,
            keepBlankLines,
//            dependency
        )
//        TODO 兼容性问题
//        return object : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
//            override fun getMinLineFeeds(): Int {
//                val superMin = super.getMinLineFeeds()
//                return if (superMin == 0) minimumLineFeeds else superMin
//            }
//        }
    }
}

/**
 * 根据配置创建或复用对齐对象。
 */
private fun createAlignment(alignOption: Boolean, defaultAlignment: Alignment?): Alignment? {
    return if (alignOption) createAlignmentOrDefault(null, defaultAlignment) else defaultAlignment
}

/**
 * 当默认对齐为空时创建新的对齐对象，否则返回默认对齐。
 */
private fun createAlignmentOrDefault(base: Alignment?, defaultAlignment: Alignment?): Alignment? {
    return defaultAlignment ?: if (base == null) Alignment.createAlignment() else Alignment.createChildAlignment(base)
}

/**
 * 仓颉 formatter 中可替换的通用对齐策略。
 */
abstract class CommonAlignmentStrategy {
    /**
     * 返回指定节点的对齐对象。
     */
    abstract fun getAlignment(node: ASTNode): Alignment?
}
