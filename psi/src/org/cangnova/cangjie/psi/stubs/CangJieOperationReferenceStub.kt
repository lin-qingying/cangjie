package org.cangnova.cangjie.psi.stubs

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.psi.CjOperationReferenceExpression

/** 运算符引用只持久化 token 身份；操作数由表达式子树承载。 */
interface CangJieOperationReferenceStub : StubElement<CjOperationReferenceExpression> {
    val operationToken: CjSingleValueToken
}
