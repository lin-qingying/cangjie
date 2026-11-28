package org.cangnova.cangjie.psi.psiUtil

import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.parsing.CjParsedDeclarationModifiers
import org.cangnova.cangjie.parsing.declarationModifiersForOwnerOrNull
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.cjmpPathIn

/**
 * 读取真实声明 owner 的 parser 返回属性；stub 路径只沿当前父子身份，不加载 AST。
 * 普通参数、访问器及缺少解析来源的二进制声明没有该结果，调用方须保留其不同语义入口。
 */
fun CjDeclaration.parsedDeclarationModifiersOrNull(): CjParsedDeclarationModifiers? {
    val stub = (this as? StubBasedPsiElement<*>)?.stub ?: return node.declarationModifiersForOwnerOrNull()
    var root: StubElement<*> = stub
    while (root.parentStub != null) root = checkNotNull(root.parentStub)
    val file = checkNotNull(root as? CangJieFileStub) { "The declaration stub does not belong to a Cangjie file." }
    val path = checkNotNull(stub.cjmpPathIn(file)) { "The declaration stub has no path in its actual file." }
    return file.cjmpRoleSnapshot?.stubDeclarationModifiers?.get(path)
}
