package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieVarOrEnumPatternStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * `VarOrEnumPattern` 是对齐官方 parser 的歧义模式节点。
 *
 * 对于 `case None => ...` 这类源码，语法阶段不能过早拍板成 binding pattern，
 * 而应先保留“它可能是变量绑定，也可能是 enum constructor”的信息，
 * 再在后续语义阶段依据当前作用域可见符号做决议。
 */
class CjVarOrEnumPattern : CjCasePattern<CangJieVarOrEnumPatternStub>, PsiNameIdentifierOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieVarOrEnumPatternStub) : super(stub, CjStubElementTypes.VAR_OR_ENUM_PATTERN)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByVarOrEnum(this, data)
    }

    val reference: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    val identifier: PsiElement?
        get() = findChildByType(org.cangnova.cangjie.lexer.CjTokens.IDENTIFIER)

    val nameAsSafeName: Name
        get() = reference?.referencedNameAsName ?: Name.identifier(identifier?.text ?: "")

    override fun getName(): String? {
        val stub = stub
        if (stub != null) return stub.name
        return nameAsSafeName.asString().takeIf { it.isNotEmpty() }
    }

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun setName(name: String): PsiElement = this
}
