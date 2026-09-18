package org.cangnova.cangjie.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.parsing.CangJieExpressionParsing
import org.cangnova.cangjie.psi.CjOperationReferenceExpression
import org.cangnova.cangjie.psi.stubs.CangJieOperationReferenceStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieOperationReferenceStubImpl

/**
 * 操作引用表达式的 stub 元素类型。
 *
 * 仅为实参位置的操作引用建 stub；序列化时持久化操作符 token 文本，
 * 反序列化时回查 [CangJieExpressionParsing.ALL_OPERATIONS] 还原原始
 * token 实例，未知文本视为二进制损坏直接失败。
 */
class CjOperationReferenceElementType(debugName: String) :
    CjStubElementType<CangJieOperationReferenceStub, CjOperationReferenceExpression>(
        debugName, CjOperationReferenceExpression::class.java, CangJieOperationReferenceStub::class.java,
    ) {
    override fun shouldCreateStub(node: ASTNode): Boolean = isValueArgumentExpression(node) && super.shouldCreateStub(node)

    override fun createStub(psi: CjOperationReferenceExpression, parentStub: StubElement<*>?): CangJieOperationReferenceStub =
        CangJieOperationReferenceStubImpl(parentStub, requireNotNull(psi.operationSignTokenType))

    override fun serialize(stub: CangJieOperationReferenceStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.operationToken.value)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieOperationReferenceStub {
        val value = dataStream.readNameString()
        val token = requireNotNull(CangJieExpressionParsing.ALL_OPERATIONS).types.filterIsInstance<CjSingleValueToken>()
            .firstOrNull { it.value == value } ?: error("Unknown persisted operation token: $value")
        return CangJieOperationReferenceStubImpl(parentStub, token)
    }
}
