package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.lang.ASTNode

/** 在声明边界内保留参数表达式的完整嵌套 Stub 结构。 */
internal fun isValueArgumentExpression(node: ASTNode): Boolean {
    var parent = node.treeParent
    while (parent != null) {
        if (parent.elementType == CjStubElementTypes.VALUE_ARGUMENT) return true
        if (CjTokenSets.DECLARATION_TYPES.contains(parent.elementType)) return false
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
