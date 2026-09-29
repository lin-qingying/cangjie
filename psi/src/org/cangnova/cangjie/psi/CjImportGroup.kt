package org.cangnova.cangjie.psi

import com.intellij.lang.ASTNode
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.stubs.CangJieImportGroupStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/** 花括号导入分组，组织限定和包前缀均属于本节点。 */
class CjImportGroup : CjElementImplStub<CangJieImportGroupStub>, CjImportPathOwner {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieImportGroupStub) : super(stub, CjStubElementTypes.IMPORT_GROUP)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R? = visitor.visitImportGroup(this, data)

    val importItems: List<CjImportItem>
        get() = getStubOrPsiChildrenAsList(CjStubElementTypes.IMPORT_ITEM)
    override val organizationReference: CjSimpleNameExpression?
        get() = importOrganizationReference(stub?.path)
    override val importedReference: CjExpression?
        get() = importLocalReference(stub?.path)
    val localFqName: FqName?
        get() { stub?.let { return it.path.localFqName }; return importFqName(importedReference) }
    val organizationName: Name?
        get() { stub?.let { return it.path.organizationName }; return organizationReference?.referencedNameAsName }
    val hasOrganizationQualifier: Boolean
        get() = stub?.path?.hasOrganizationQualifier ?: (node.findChildByType(CjTokens.DOUBLE_COLON) != null)
    val isValidSyntax: Boolean
        get() = stub?.path?.isValidSyntax ?: !hasImportContainerErrors(this)
}
