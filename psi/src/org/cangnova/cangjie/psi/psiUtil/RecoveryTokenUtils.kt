package org.cangnova.cangjie.psi.psiUtil

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

/**
 * 查找当前语法层的 token，只穿透 parser 为完整 token 诊断创建的 ERROR_ELEMENT。
 * 不进入注解、表达式或嵌套声明；错误包装不改变修饰符/声明关键字的所属层级。
 */
internal fun ASTNode.findChildTokenThroughErrors(types: TokenSet): ASTNode? {
    var child = firstChildNode
    while (child != null) {
        if (types.contains(child.elementType)) return child
        if (child.elementType == TokenType.ERROR_ELEMENT) {
            child.findChildTokenThroughErrors(types)?.let { return it }
        }
        child = child.treeNext
    }
    return null
}
