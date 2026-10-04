package org.cangnova.cangjie.ide.completion.impl.shared.context

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjContainerNode
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationWithInitializer
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjValueArgument

/**
 * 补全位置的语义分类。
 *
 * 对齐 Kotlin `KotlinPositionContext`：这里只做**位置形态**分类，
 * 不产出候选，也不做可见性判断。仓颉特有的形态（`match`、命名实参、CDoc）
 * 单独登记，不按 Kotlin 的 `when`/trailing lambda 直接映射。
 */
enum class CompletionPositionKind {
    /** 普通表达式位置。 */
    EXPRESSION,

    /** 类型引用位置。 */
    TYPE,

    /** 成员访问位置：`receiver.` 之后。 */
    MEMBER_ACCESS,

    /** import 语句位置。 */
    IMPORT,

    /** 包名位置。 */
    PACKAGE,

    /** 声明名位置：函数/类/变量/属性声明的标识符。 */
    DECLARATION_NAME,

    /** 命名实参位置：形参名 + 冒号。 */
    NAMED_ARGUMENT,

    /** 运算符位置。 */
    OPERATOR,

    /** CDoc 标签或引用位置。 */
    DOCUMENTATION,

    /** 宏 Tokens 输入位置。 */
    MACRO_TOKENS,

    /** 函数体内部。 */
    FUNCTION_BODY,
}

/**
 * 位置分类结果。
 *
 * @param kind 位置语义分类。
 * @param receiverExpression 成员访问的接收者表达式；非成员访问为 `null`。
 * @param declaration 当前位置所属的声明；不属于任何声明时为 `null`。
 */
data class CompletionPositionContext(
    val kind: CompletionPositionKind,
    val receiverExpression: PsiElement? = null,
    val declaration: CjElement? = null,
)

/**
 * 按 PSI 形态判定补全位置。
 *
 * 只读取位置附近的语法结构，不做名称解析：把「这里是什么形状的槽位」与
 * 「这里能补出什么」分开，才能让候选生成按分类选择而不重复判断语法。
 */
object CompletionPositionContexts {
    /**
     * 判定 [position] 的补全位置分类。
     *
     * [position] 可以是光标处的叶子节点，也可以是已完成 dummy 修正的元素。
     */
    fun classify(position: PsiElement): CompletionPositionContext {
        val declaration = position.containingDeclarationOrNull()

        if (position.insideOf<CjImportDirective>() != null) {
            return CompletionPositionContext(CompletionPositionKind.IMPORT, declaration = declaration)
        }
        if (position.insideOf<PsiComment>() != null) {
            return CompletionPositionContext(CompletionPositionKind.DOCUMENTATION, declaration = declaration)
        }
        position.qualifiedReceiverOrNull()?.let { receiver ->
            return CompletionPositionContext(CompletionPositionKind.MEMBER_ACCESS, receiver, declaration)
        }
        if (position.insideOf<CjTypeReference>() != null) {
            return CompletionPositionContext(CompletionPositionKind.TYPE, declaration = declaration)
        }
        position.namedArgumentOrNull()?.let {
            return CompletionPositionContext(CompletionPositionKind.NAMED_ARGUMENT, declaration = declaration)
        }
        // 初始化槽位只看直接父节点：`CjNamedFunction` 本身也是带初始化器的声明，
        // 沿祖先链上溯会把整个函数体误判成初始化表达式位置。
        if (position.parent is CjDeclarationWithInitializer) {
            return CompletionPositionContext(CompletionPositionKind.EXPRESSION, declaration = declaration)
        }
        if (position.isOnDeclarationIdentifier()) {
            return CompletionPositionContext(CompletionPositionKind.DECLARATION_NAME, declaration = declaration)
        }
        if (position.insideOf<CjBlockExpression>() != null) {
            return CompletionPositionContext(CompletionPositionKind.FUNCTION_BODY, declaration = declaration)
        }
        if (position.insideOf<CjBinaryExpression>() != null) {
            return CompletionPositionContext(CompletionPositionKind.OPERATOR, declaration = declaration)
        }
        return CompletionPositionContext(CompletionPositionKind.EXPRESSION, declaration = declaration)
    }

    /**
     * 判断位置是否处于 `receiver.` 形式成员访问中，并返回接收者表达式。
     */
    private fun PsiElement.qualifiedReceiverOrNull(): PsiElement? {
        var current: PsiElement = this
        while (true) {
            val parent = current.parent as? PsiElement ?: return null
            if (parent is CjContainerNode) {
                current = parent
                continue
            }
            val qualifier = parent as? CjDotQualifiedExpression ?: return null
            // 只在光标落在选择器一侧时判定为成员访问：落在接收者一侧属于普通表达式位置。
            val selector = qualifier.selectorExpression ?: return null
            val isOnSelector = selector === current || selector.parent === current
            return if (isOnSelector) qualifier.receiverExpression else null
        }
    }

    /**
     * 判断位置是否在 import 指令内部。
     */
    private inline fun <reified T : PsiElement> PsiElement.insideOf(): T? {
        var current: PsiElement = this
        while (true) {
            if (current is T) return current
            current = current.parent as? PsiElement ?: return null
        }
    }

    /**
     * 取得当前位置所属的声明。
     */
    private fun PsiElement.containingDeclarationOrNull(): CjElement? =
        com.intellij.psi.util.PsiTreeUtil.getParentOfType(this, CjDeclaration::class.java)

    /**
     * 判断位置是否是命名实参的形参名位置。
     */
    private fun PsiElement.namedArgumentOrNull(): PsiElement? {
        val valueArgument = insideOf<CjValueArgument>() ?: return null
        return valueArgument.takeIf { it.isNamed() }
    }

    /**
     * 判断位置是否位于声明的标识符上。
     *
     * 声明名补全只在标识符本身触发；表达式位置的同名文本不属于该形态。
     */
    private fun PsiElement.isOnDeclarationIdentifier(): Boolean {
        val owner = this as? PsiNameIdentifierOwner ?: return false
        val identifier = owner.nameIdentifier ?: return false
        return identifier === this || identifier.parent === this
    }
}
