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

/**
 * LightTree 修饰符解析（对齐 PSI 版的 CjModifierList 行为）。
 *
 * 从 [CjNodeTypes.MODIFIER_LIST] 子树中提取修饰符关键字 Token，
 * 判断可见性、抽象性、静态、可变等修饰符。
 */
class LightTreeModifierList(
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val modifierListNode: LighterASTNode?,
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

    fun hasModifier(token: com.intellij.psi.tree.IElementType): Boolean =
        token in modifierTokens

    // ===== 可见性 =====

    val visibility: Visibility
        get() = when {
            hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
            hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
            hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
            else -> Visibilities.Internal // 默认 internal
        }

    val isVisibilityExplicit: Boolean
        get() = hasModifier(CjTokens.PUBLIC_KEYWORD)
                || hasModifier(CjTokens.PRIVATE_KEYWORD)
                || hasModifier(CjTokens.PROTECTED_KEYWORD)
                || hasModifier(CjTokens.INTERNAL_KEYWORD)

    // ===== 模态（modality） =====

    val isAbstract: Boolean get() = hasModifier(CjTokens.ABSTRACT_KEYWORD)
    val isOpen: Boolean get() = hasModifier(CjTokens.OPEN_KEYWORD)
    val isSealed: Boolean get() = hasModifier(CjTokens.SEALED_KEYWORD)

    val isModalityExplicit: Boolean
        get() = isAbstract || isOpen || isSealed

    // ===== 其他修饰符 =====

    val isStatic: Boolean get() = hasModifier(CjTokens.STATIC_KEYWORD)
    val isConst: Boolean get() = hasModifier(CjTokens.CONST_KEYWORD)
    val isMut: Boolean get() = hasModifier(CjTokens.MUT_KEYWORD)
    val isOverride: Boolean get() = hasModifier(CjTokens.OVERRIDE_KEYWORD)
    val isRedef: Boolean get() = hasModifier(CjTokens.REDEF_KEYWORD)
    val isOperator: Boolean get() = hasModifier(CjTokens.OPERATOR_KEYWORD)
    val isUnsafe: Boolean get() = hasModifier(CjTokens.UNSAFE_KEYWORD)
    val isForeign: Boolean get() = hasModifier(CjTokens.FOREIGN_KEYWORD)

    /**
     * 转换为 [CfirDeclarationStatus]，与 [AbstractRawCfirBuilder.buildDeclarationStatus] 对齐。
     */
    fun toDeclarationStatus(
        inLocalContext: Boolean,
        inInterfaceContext: Boolean,
        defaultVisibility: Visibility? = null,
    ): CfirDeclarationStatus {
        val effectiveDefaultVisibility = defaultVisibility ?: when {
            inLocalContext -> Visibilities.Local
            inInterfaceContext -> Visibilities.Public
            else -> Visibilities.Internal
        }
        val effectiveVisibility = if (isVisibilityExplicit) visibility else effectiveDefaultVisibility
        val status = CfirDeclarationStatusImpl(
            visibility = effectiveVisibility,
            modality = Modality.convertFromFlags(isSealed, isAbstract, isOpen),
        )
        status.isAbstract = isAbstract
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
        return status
    }

    companion object {
        /** 从声明节点中提取 MODIFIER_LIST 子节点并构建 [LightTreeModifierList]。 */
        fun from(
            tree: FlyweightCapableTreeStructure<LighterASTNode>,
            declarationNode: LighterASTNode,
        ): LightTreeModifierList {
            val modifierList = tree.findChildByType(declarationNode, CjNodeTypes.MODIFIER_LIST)
            return LightTreeModifierList(tree, modifierList)
        }
    }
}
