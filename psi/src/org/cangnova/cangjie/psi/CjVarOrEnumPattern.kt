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

    /**
     * 实现 `accept` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? {
        return visitor.visitPatternByVarOrEnum(this, data)
    }

    /**
     * 保存 `reference`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val reference: CjSimpleNameExpression?
        get() = findChildByType(CjNodeTypes.REFERENCE_EXPRESSION)

    /**
     * 保存 `identifier`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val identifier: PsiElement?
        get() = findChildByType(org.cangnova.cangjie.lexer.CjTokens.IDENTIFIER)

    /**
     * 保存 `nameAsSafeName`，供仓颉 PSI流程读取节点结构或语义信息。
     */
    val nameAsSafeName: Name
        get() = reference?.referencedNameAsName ?: Name.identifier(identifier?.text ?: "")

    /**
     * 实现 `getName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String? {
        val stub = stub
        if (stub != null) return stub.name
        return nameAsSafeName.asString().takeIf { it.isNotEmpty() }
    }

    /**
     * 实现 `getNameIdentifier` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getNameIdentifier(): PsiElement? = identifier

    /**
     * 实现 `setName` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun setName(name: String): PsiElement = this
}
