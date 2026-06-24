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

package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * LightTree 修饰符解析（对齐 PSI 版的 CjModifierList 行为）。
 *
 * 从 [CjNodeTypes.MODIFIER_LIST] 子树中提取修饰符关键字 Token，
 * 判断可见性、抽象性、静态、可变等修饰符。
 *
 * @property tree 当前 LightTree 树结构。
 * @property modifierListNode modifier list 节点；声明没有 modifier list 时为 null。
 * @property annotations 声明或参数上采集到的 annotation 节点列表。
 */
class LightTreeModifierList(
    /** 当前 LightTree 树结构。 */
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    /** modifier list 节点；声明没有 modifier list 时为 null。 */
    private val modifierListNode: LighterASTNode?,
    /** 声明或参数上采集到的 annotation 节点列表。 */
    val annotations: List<LighterASTNode>,
) {
    /** 修饰符 Token 类型集合（用于快速查找） */
    private val modifierTokens: Set<com.intellij.psi.tree.IElementType> by lazy {
        if (modifierListNode == null) return@lazy emptySet()
        val tokens = mutableSetOf<com.intellij.psi.tree.IElementType>()
        tree.forEachChildren(modifierListNode) { child ->
            tokens.add(child.tokenType)
        }
        tokens
    }

    /** 判断 modifier list 是否包含指定 [token]。 */
    fun hasModifier(token: com.intellij.psi.tree.IElementType): Boolean =
        token in modifierTokens

    // ===== 可见性 =====

    /** 声明显式可见性；没有显式可见性时返回 internal。 */
    val visibility: Visibility
        get() = when {
            hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
            hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
            hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
            else -> Visibilities.Internal // 默认 internal
        }

    /** 当前 modifier list 是否显式声明了可见性。 */
    val isVisibilityExplicit: Boolean
        get() = hasModifier(CjTokens.PUBLIC_KEYWORD)
                || hasModifier(CjTokens.PRIVATE_KEYWORD)
                || hasModifier(CjTokens.PROTECTED_KEYWORD)
                || hasModifier(CjTokens.INTERNAL_KEYWORD)

    // ===== 模态（modality） =====

    /** 是否包含 abstract modifier。 */
    val isAbstract: Boolean get() = hasModifier(CjTokens.ABSTRACT_KEYWORD)
    /** 是否包含 open modifier。 */
    val isOpen: Boolean get() = hasModifier(CjTokens.OPEN_KEYWORD)
    /** 是否包含 sealed modifier。 */
    val isSealed: Boolean get() = hasModifier(CjTokens.SEALED_KEYWORD)

    /** 是否显式声明了 modality。 */
    val isModalityExplicit: Boolean
        get() = isAbstract || isOpen || isSealed

    // ===== 其他修饰符 =====

    /** 是否包含 static modifier。 */
    val isStatic: Boolean get() = hasModifier(CjTokens.STATIC_KEYWORD)
    /** 是否包含 const modifier。 */
    val isConst: Boolean get() = hasModifier(CjTokens.CONST_KEYWORD)
    /** 是否包含 mut modifier。 */
    val isMut: Boolean get() = hasModifier(CjTokens.MUT_KEYWORD)
    /** 是否包含 override modifier。 */
    val isOverride: Boolean get() = hasModifier(CjTokens.OVERRIDE_KEYWORD)
    /** 是否包含 redef modifier。 */
    val isRedef: Boolean get() = hasModifier(CjTokens.REDEF_KEYWORD)
    /** 是否包含 operator modifier。 */
    val isOperator: Boolean get() = hasModifier(CjTokens.OPERATOR_KEYWORD)
    /** 是否包含 unsafe modifier。 */
    val isUnsafe: Boolean get() = hasModifier(CjTokens.UNSAFE_KEYWORD)
    /** 是否包含 foreign modifier。 */
    val isForeign: Boolean get() = hasModifier(CjTokens.FOREIGN_KEYWORD)

    /** 按源码顺序暴露声明/参数修饰符文本，供 construction-only surface 携带。 */
    val modifierTexts: List<String> by lazy {
        if (modifierListNode == null) return@lazy emptyList()
        val result = mutableListOf<String>()
        tree.forEachChildren(modifierListNode) { child ->
            if (child.tokenType != CjStubElementTypes.ANNOTATIONS && child.tokenType != CjNodeTypes.ANNOTATION) {
                result.add(child.tokenType.toString())
            }
        }
        result
    }

    /**
     * 转换为 [CfirDeclarationStatus]，与 [AbstractRawCfirBuilder.buildDeclarationStatus] 对齐。
     */
    fun toDeclarationStatus(
        inLocalContext: Boolean,
        inInterfaceContext: Boolean,
        defaultVisibility: Visibility? = null,
        isDefault: Boolean = false,
        isImplicitAbstract: Boolean = false,
    ): CfirDeclarationStatus {
        val effectiveDefaultVisibility = defaultVisibility ?: when {
            inLocalContext -> Visibilities.Local
            inInterfaceContext -> Visibilities.Public
            else -> Visibilities.Internal
        }
        val effectiveVisibility = if (isVisibilityExplicit) visibility else effectiveDefaultVisibility
        val effectiveIsAbstract = isAbstract || isImplicitAbstract
        val status = CfirDeclarationStatusImpl(
            visibility = effectiveVisibility,
            modality = Modality.convertFromFlags(isSealed, effectiveIsAbstract, isOpen),
        )
        status.isAbstract = effectiveIsAbstract
        status.isOpen = isOpen
        status.isSealed = isSealed
        status.isVisibilityExplicit = isVisibilityExplicit
        status.isModalityExplicit = isModalityExplicit
        status.isStatic = isStatic
        status.isConst = isConst
        status.isMut = isMut
        status.isOverride = isOverride
        status.isRedef = isRedef
        status.isOperator = isOperator
        status.isUnsafe = isUnsafe
        status.isForeign = isForeign
        status.isDefault = isDefault
        return status
    }

    /** [LightTreeModifierList] 构造与 annotation 收集工具。 */
    companion object {
        /** 从声明节点中提取 MODIFIER_LIST 子节点并构建 [LightTreeModifierList]。 */
        fun from(
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
            declarationNode: LighterASTNode,
        ): LightTreeModifierList {
            val modifierList = tree.findChildByType(declarationNode, CjNodeTypes.MODIFIER_LIST)
            val annotations = buildList {
                collectAnnotationsFrom(tree, declarationNode, this)
                if (modifierList != null) {
                    collectAnnotationsFrom(tree, modifierList, this)
                }
            }
            return LightTreeModifierList(tree, modifierList, annotations)
        }

        /** 从 [node] 的直接子树中收集 annotation 与 macro expression annotation 包装。 */
        private fun collectAnnotationsFrom(
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
            node: LighterASTNode,
            result: MutableList<LighterASTNode>,
        ) {
            val initializerBoundary = node.firstDirectChildOffset(tree, CjTokens.EQ)
            tree.forEachChildren(node) { child ->
                when (child.tokenType) {
                    CjStubElementTypes.ANNOTATIONS -> tree.forEachChildren(child) { annotation ->
                        if (annotation.tokenType == CjNodeTypes.ANNOTATION || annotation.tokenType == CjNodeTypes.MACRO_EXPRESSION) {
                            result.add(annotation)
                        }
                    }
                    CjNodeTypes.ANNOTATION,
                    CjNodeTypes.MACRO_EXPRESSION,
                    -> if (initializerBoundary == null || tree.getStartOffset(child) < initializerBoundary) {
                        result.add(child)
                    }
                }
            }
        }

        /** 返回第一个直接子节点 token type 为 [tokenType] 的起始偏移。 */
        private fun LighterASTNode.firstDirectChildOffset(
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
            tokenType: com.intellij.psi.tree.IElementType,
        ): Int? {
            tree.forEachChildren(this) { child ->
                if (child.tokenType == tokenType) return tree.getStartOffset(child)
            }
            return null
        }
    }
}
