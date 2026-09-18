package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.psi.CjExpression

/** 参数表达式的嵌套节点必须一起建桩，不能把叶子提升到参数下而丢失运算结构。 */
internal fun isValueArgumentExpression(node: ASTNode): Boolean {
    var parent = node.treeParent
    while (parent != null) {
        if (parent.elementType == CjStubElementTypes.VALUE_ARGUMENT) return true
        if (parent.psi !is CjExpression) return false
        parent = parent.treeParent
    }
    return false
}

/** 仅在可持久化的参数表达式中保存复合节点，不为函数实现体建立表达式索引。 */
class CjArgumentExpressionElementType<T : org.cangnova.cangjie.psi.CjElementImplStub<out com.intellij.psi.stubs.StubElement<*>>>(
    debugName: String,
    psiClass: Class<T>,
) : CjPlaceHolderStubElementType<T>(debugName, psiClass) {
    override fun shouldCreateStub(node: ASTNode): Boolean = isValueArgumentExpression(node) && super.shouldCreateStub(node)
}
