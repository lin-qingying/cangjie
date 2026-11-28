package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * 已解析但不具有合法声明身份的源码槽位。
 * 可以保留恢复后的源码子树供 IDE 使用，但不暴露名称、构造器或参数签名 API；
 * stub 只保存这个声明边界，raw 转换也不遍历其中的签名残留。
 */
class CjInvalidDeclaration : CjDeclarationStub<CangJiePlaceHolderStub<CjInvalidDeclaration>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjInvalidDeclaration>) :
        super(stub, CjStubElementTypes.INVALID_DECLARATION)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitDeclaration(this, data)
}
