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
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.psiUtil.children
import org.cangnova.cangjie.psi.stubs.elements.CjModifierListElementType
import com.intellij.formatting.*
import com.intellij.formatting.DependentSpacingRule.Anchor
import com.intellij.formatting.DependentSpacingRule.Trigger
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

import kotlin.math.max

/**
 * 创建右花括号前依赖换行状态的 spacing。
 */
fun CommonCodeStyleSettings.createSpaceBeforeRBrace(numSpacesOtherwise: Int, textRange: TextRange): Spacing? {
    return Spacing.createDependentLFSpacing(
        numSpacesOtherwise, numSpacesOtherwise, textRange,
        KEEP_LINE_BREAKS,
        KEEP_BLANK_LINES_BEFORE_RBRACE
    )
}

/**
 * 仓颉 formatter 的 spacing 规则容器。
 */
class CangJieSpacingBuilder(
    /** 通用代码风格设置。 */
    val commonCodeStyleSettings: CommonCodeStyleSettings,
    /** spacing 创建时需要的平台适配工具。 */
    val spacingBuilderUtil: CangJieSpacingBuilderUtil
) {
    /** 按注册顺序保存的 spacing builder。 */
    private val builders = ArrayList<Builder>()

    /**
     * 可根据相邻 AST block 计算 spacing 的规则构建器。
     */
    private interface Builder {
        /**
         * 返回 parent 下 left/right block 之间的 spacing。
         */
        fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing?
    }

    /**
     * IntelliJ 原生 SpacingBuilder 的仓颉包装。
     */
    inner class BasicSpacingBuilder : SpacingBuilder(commonCodeStyleSettings), Builder {
        /**
         * 委托给 IntelliJ 原生 spacing builder。
         */
        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            return super.getSpacing(parent, left, right)
        }
    }

    /**
     * 自定义 spacing 规则的匹配条件。
     */
    private data class Condition(
        /** 父 block 的元素类型条件。 */
        val parent: IElementType? = null,
        /** 左 block 的元素类型条件。 */
        val left: IElementType? = null,
        /** 右 block 的元素类型条件。 */
        val right: IElementType? = null,
        /** 父 block 的元素类型集合条件。 */
        val parentSet: TokenSet? = null,
        /** 左 block 的元素类型集合条件。 */
        val leftSet: TokenSet? = null,
        /** 右 block 的元素类型集合条件。 */
        val rightSet: TokenSet? = null
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Boolean {
        /**
         * 判断 parent/left/right 是否满足所有已声明条件。
         */
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Boolean =
            (parent == null || p.requireNode().elementType == parent) &&
                    (left == null || l.requireNode().elementType == left) &&
                    (right == null || r.requireNode().elementType == right) &&
                    (parentSet == null || parentSet.contains(p.requireNode().elementType)) &&
                    (leftSet == null || leftSet.contains(l.requireNode().elementType)) &&
                    (rightSet == null || rightSet.contains(r.requireNode().elementType))
    }

    /**
     * 自定义 spacing 规则，由条件列表和动作组成。
     */
    private data class Rule(
        /** 规则必须满足的条件列表。 */
        val conditions: List<Condition>,
        /** 条件满足后执行的 spacing 计算动作。 */
        val action: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Spacing? {
        /**
         * 条件全部满足时执行 spacing 动作。
         */
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Spacing? =
            if (conditions.all { it(p, l, r) }) action(p, l, r) else null
    }

    /**
     * 支持闭包规则的自定义 spacing builder。
     */
    inner class CustomSpacingBuilder : Builder {
        /** 已注册的自定义 spacing 规则。 */
        private val rules = ArrayList<Rule>()
        /** 当前正在累积的匹配条件。 */
        private var conditions = ArrayList<Condition>()

        /**
         * 依次尝试自定义规则并返回第一个非空 spacing。
         */
        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            for (rule in rules) {
                val spacing = rule(parent, left, right)
                if (spacing != null) {
                    return spacing
                }
            }
            return null
        }

        /**
         * 累加 parent/left/right 位置条件。
         */
        fun inPosition(
            parent: IElementType? = null, left: IElementType? = null, right: IElementType? = null,
            parentSet: TokenSet? = null, leftSet: TokenSet? = null, rightSet: TokenSet? = null
        ): CustomSpacingBuilder {
            conditions.add(Condition(parent, left, right, parentSet, leftSet, rightSet))
            return this
        }

        /**
         * 根据父 block 是否换行创建依赖换行 spacing。
         */
        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int, allowBlankLines: Boolean = true) {
            newRule { p, _, _ ->
                Spacing.createDependentLFSpacing(
                    numSpacesOtherwise, numSpacesOtherwise, p.textRange,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    if (allowBlankLines) commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE else 0
                )
            }
        }

        /**
         * 当左侧声明跨行时插入指定空行数。
         */
        fun emptyLinesIfLineBreakInLeft(emptyLines: Int, numberOfLineFeedsOtherwise: Int = 1, numSpacesOtherwise: Int = 0) {
            newRule { _: ASTBlock, left: ASTBlock, _: ASTBlock ->
                val lastChild = left.node?.psi?.lastChild
                val leftEndsWithComment = lastChild is PsiComment && lastChild.tokenType == CjTokens.EOL_COMMENT
                val dependentSpacingRule = DependentSpacingRule(Trigger.HAS_LINE_FEEDS).registerData(Anchor.MIN_LINE_FEEDS, emptyLines + 1)
                val textRange = left.node
                    ?.startOfDeclaration()
                    ?.startOffset
                    ?.let { TextRange.create(it, left.textRange.endOffset) }
                    ?: left.textRange

                spacingBuilderUtil.createLineFeedDependentSpacing(
                    numSpacesOtherwise,
                    numSpacesOtherwise,
                    if (leftEndsWithComment) max(1, numberOfLineFeedsOtherwise) else numberOfLineFeedsOtherwise,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                    textRange,
                    dependentSpacingRule
                )
            }
        }

        /**
         * 注册固定 spacing 规则。
         */
        fun spacing(spacing: Spacing) {
            newRule { _, _, _ -> spacing }
        }

        /**
         * 注册自定义 spacing 计算规则。
         */
        fun customRule(block: (parent: ASTBlock, left: ASTBlock, right: ASTBlock) -> Spacing?) {
            newRule(block)
        }

        /**
         * 使用当前条件创建规则，并清空条件累积区。
         */
        private fun newRule(rule: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?) {
            val savedConditions = ArrayList(conditions)
            rules.add(Rule(savedConditions, rule))
            conditions.clear()
        }
    }

    /**
     * 计算 parent 下两个相邻子 block 之间的 spacing。
     */
    fun getSpacing(parent: Block, child1: Block?, child2: Block): Spacing? {
        if (parent !is ASTBlock || child1 !is ASTBlock || child2 !is ASTBlock) {
            return null
        }

        for (builder in builders) {
            val spacing = builder.getSpacing(parent, child1, child2)

            if (spacing != null) {

                if (child1.requireNode().elementType == CjTokens.EOL_COMMENT && spacing.toString().contains("minLineFeeds=0")) {
                    val isBeforeBlock =
                        child2.requireNode().elementType == CjNodeTypes.BLOCK || child2.requireNode().firstChildNode
                            ?.elementType == CjNodeTypes.BLOCK
                    val keepBlankLines = if (isBeforeBlock) 0 else commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    return createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = keepBlankLines)
                }
                return spacing
            }
        }
        return null
    }

    /**
     * 注册一组 IntelliJ 原生 spacing builder 规则。
     */
    fun simple(init: BasicSpacingBuilder.() -> Unit) {
        val builder = BasicSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    /**
     * 注册一组仓颉自定义 spacing 规则。
     */
    fun custom(init: CustomSpacingBuilder.() -> Unit) {
        val builder = CustomSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    /**
     * 创建标准 spacing 对象。
     */
    fun createSpacing(
        minSpaces: Int,
        maxSpaces: Int = minSpaces,
        minLineFeeds: Int = 0,
        keepLineBreaks: Boolean = commonCodeStyleSettings.KEEP_LINE_BREAKS,
        keepBlankLines: Int = commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
    ): Spacing {
        return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)
    }
}

/**
 * spacing builder 依赖的平台能力抽象。
 */
interface CangJieSpacingBuilderUtil {
    /**
     * 创建依赖换行状态的 spacing。
     */
    fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing

    /**
     * 返回指定节点前一个非空白叶子节点。
     */
    fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode?

    /**
     * 判断节点是否为空白或空节点。
     */
    fun isWhitespaceOrEmpty(node: ASTNode?): Boolean
}

/**
 * 创建并初始化仓颉 spacing builder。
 */
fun rules(
    commonCodeStyleSettings: CommonCodeStyleSettings,
    builderUtil: CangJieSpacingBuilderUtil,
    init: CangJieSpacingBuilder.() -> Unit
): CangJieSpacingBuilder {
    val builder = CangJieSpacingBuilder(commonCodeStyleSettings, builderUtil)
    builder.init()
    return builder
}

/**
 * 返回声明中跳过修饰符和空白注释后的第一个有效 AST 节点。
 */
internal fun ASTNode.startOfDeclaration(): ASTNode? = children().firstOrNull {
    val elementType = it.elementType
    elementType !is CjModifierListElementType<*> && elementType !in CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET
}
