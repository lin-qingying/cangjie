package org.cangnova.cangjie.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.psi.CjOperationReferenceExpression
import org.cangnova.cangjie.psi.stubs.CangJieOperationReferenceStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 操作引用表达式的 stub 实现。
 *
 * 仅持有操作符 token（如 `+`、`..`）；[copyInto] 复制时保留该 token，
 * 供索引端在无 PSI 时还原操作符语义。
 */
class CangJieOperationReferenceStubImpl(
    parent: StubElement<*>?,
    override val operationToken: CjSingleValueToken,
) : CangJieStubBaseImpl<CjOperationReferenceExpression>(parent, CjStubElementTypes.OPERATION_REFERENCE),
    CangJieOperationReferenceStub {
    override fun copyInto(newParent: StubElement<*>?): CangJieOperationReferenceStubImpl =
        CangJieOperationReferenceStubImpl(newParent, operationToken)
}
